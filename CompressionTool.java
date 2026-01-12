import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompressionTool{
    private String fileUrl;
    public CompressionTool(String fileUrl){
        this.fileUrl = fileUrl;
    }
    public TreeMap<Character,Integer> charactersOcurrences(){
        TreeMap<Character,Integer> map = new TreeMap<>();
        try (BufferedInputStream bf = new BufferedInputStream(new FileInputStream(new File(fileUrl)))) {
            while (true) {
                int b = bf.read();
                if(b == -1) break;
                Character ch = (char)b;
                if(map.containsKey(ch)){
                    map.put(ch,map.get(ch)+1);
                }
                else{
                    map.put(ch,1);
                }
            }            
        } catch (FileNotFoundException e) {
            System.err.println("file not found");
        }
        catch(IOException e){
            System.err.println("error while reading the file");
        }
        return map;
    }

    //header as freq table or tree
    static void writeHeader1(String url,/*TreeMap<Character,Integer>*/String header){
        //File can t be declared separately inside try with ressources cuz it is not autocloseable
        try (FileOutputStream out = new FileOutputStream(new File(url));
        BufferedOutputStream bf = new BufferedOutputStream(out)){
            // for(Character c : header.keySet()){
            //     bf.write(new String(c + ":" + header.get(c)).getBytes(StandardCharsets.UTF_8));
            // }

            writeBits(bf, header);
            bf.write(0);
            String s = "";
            String s1 = "";
            int buffer = 0;
            int bitCount = 0;
            int c = 0;
            for (int i = 0; i < header.length(); i++) {
                if(header.charAt(i) == '0' || header.charAt(i) == '1'){
                    buffer = (buffer << 1) | header.charAt(i) - '0';
                    bitCount++;
                    c++;
                    s += header.charAt(i);
                }
                else{
                    // value = (value << 8) | (byte)header.charAt(i);
                    buffer = (buffer << 8) | header.charAt(i) & 0xFF;
                    bitCount += 8;
                    c += 8;
                    s += Integer.toBinaryString(header.charAt(i));
                }
                while(bitCount >= 8){
                    int shift = bitCount-8;
                    int temp = (buffer >> shift);
                    bitCount -= 8;
                    bf.write(temp & 0xFF);
                    s1 += Integer.toBinaryString(temp);
                    // buffer = buffer & ((int)(Math.pow(2, bitCount))-1);
                    //pow(2,n) = (1 << bitCount) and -1 is to make n 1s to pick only the last bits
                    //we need only binary num with 1s 11=3 111=7 etc
                    buffer &= ((1 << bitCount)-1);
                }
            }
            if (bitCount>0 && bitCount<8) {
                buffer <<= (8-bitCount);
                c += (8-bitCount);
                bf.write(buffer & 0xFF);
                s1 += Integer.toBinaryString(buffer);
            }
            System.out.println(c);
            System.out.println(s);
            System.out.println(s1);
        }
        catch(IOException e){
            System.err.println("problem with writing");
        } 
    }

    //header as freq table or tree (tree in our case)
    static void writeHeader(BufferedOutputStream bos,String header,int uncompressed,int compressed){
        //File can t be declared separately inside try with ressources cuz it is not autocloseable
        try{
            // for (int i = 3; i >= 0; i--) {
            //     bf.write((uncompressed >> i*8) & 0xFF);
            // }
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(compressed);
            dos.writeInt(header.length());
            dos.writeInt(uncompressed);
            System.out.println(compressed + " " + uncompressed + " " + header.length());
            writeBits(bos, header);
            bos.write(0);
            //buffere need to be closed so it can flush data into the file
            // bos.flush();
        }
        catch(IOException e){
            System.err.println("problem with writing");
        } 
    }

    static void writeContent(BufferedOutputStream bos,String content){
        writeBits(bos, content);
    }

    static void writeHeaderAndContent(String url,String header,String encoded,int uncompressed,int compressed){
        try (FileOutputStream out = new FileOutputStream(new File(url));
            BufferedOutputStream bos = new BufferedOutputStream(out);) {
            writeHeader(bos, header, uncompressed, compressed);
            writeContent(bos, encoded);
            bos.close();
        } catch (Exception e) {
            System.err.println("problem with writing");
        }
    }

    static void writeBits(BufferedOutputStream bf,String bits){
        try  {
            String s = "";
            System.out.println(bits.length());
            for(int i=0;i<bits.length();i++){
                s = s+bits.charAt(i);
                // System.out.println(s);
                if (s.length()==8) {
                    // byte b = (byte) Integer.parseInt(s, 2); //it parses the string in base 2
                    //use int as temporary container to manage bits int is 32 bits but in the file just the 8 bits will be written 0s in the left will be ingnored
                    // 00000000 00000000 00000001 01101100 only the last 8 bits will be written cuz write() only write 8 bits
                    int value = 0;
                    for(int j=0;j<8;j++){
                        //left shifting for each step and use bitwise OR to make shifting by 1 or 0 depending on the char soming from s is 0 or 1
                        value = (value << 1) | (s.charAt(j)-'0');
                        // System.out.println(value);
                    }
                    s = "";
                    byte b = (byte) value;
                    //0xFF to pick the last 8 bits it means 255 (unsigned) or -1 (signed) it means 11111111 each F has 4 bits
                    // means if we talks about 32 bits we got 00000 .... 11111111 (value is int and coming with 32 bits we pass it to byte and apply &) and & make us pick the last 8 bits and cancel all the other as write() only accepts 8 bits
                    bf.write( b & 0xFF);
                    //bf.write( value & 0xFF); //also correct
                }           
            }
            int value = 0;
            if (!s.isEmpty()) {
                // System.out.println(s);
                s += "00000000".substring(s.length());
                // s += s1;
                // System.out.println(s);
                for (int j=0;j<s.length();j++) {
                    value = (value << 1) | s.charAt(j)-'0';
                }
                bf.write(value & 0xFF);
            }
        } 
        catch (IOException e) {
            System.err.println("problem with writing..");
        }
    }

    static String extractText(String url){
        String s = "";
        try (FileReader fr = new FileReader(url); BufferedReader bf = new BufferedReader(fr)) {
            Stream<String> stream = bf.lines();
            // stream.forEach(line -> s+=line);
            s = stream.collect(Collectors.joining());
            // String line;
            // while ((line = bf.readLine()) != null) {
            //     s += line;
            // }
        } catch (Exception e) {
            System.err.println("problem with reading..");
        }
        return s;
    }

    //dfs or post traversal
    static void ExtractHeaderFromTree(/*String s*/StringBuilder sb,HuffmanTree tree){ // String is immutable and when changing a string variable value we create another string internally it is just one thread immutability prevent multithreading and guarantee safety so basically the variable passed from main is passed by value and the changes inside the method doesn t affect it cuz it still reference the old value
        //String builder in the other hand is mutable so we can change the object no need to create another one internally
        if (tree == null) return;
        
        if(tree.getRoot().isLeaf()){
            HuffmanLeafNode leaf = (HuffmanLeafNode)tree.getRoot();
            sb.append("1");
            String bits = String.format("%8s", Integer.toBinaryString(leaf.getChar()))
                     .replace(' ', '0');
            sb.append(bits);         
            // sb.append(leaf.getChar());         
        }
        else{
            sb.append("0");
            HuffmanInternalNode internalNode = (HuffmanInternalNode)tree.getRoot();
            ExtractHeaderFromTree(sb, new HuffmanTree(internalNode.getLeft()));
            ExtractHeaderFromTree(sb, new HuffmanTree(internalNode.getRight()));
        }
    }

    static void testWrite(String url,String content){
        try (FileOutputStream out = new FileOutputStream(new File(url));
            BufferedOutputStream bfOut = new BufferedOutputStream(out)) {
            bfOut.write(0xAF);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    static HashMap<String,Object> readCompressedFile(String url){
        HashMap<String,Object> map = new HashMap<>();
        try (FileInputStream in = new FileInputStream(new File(url));
            BufferedInputStream bf = new BufferedInputStream(in)) {
                DataInputStream dis = new DataInputStream(in); 
                boolean delimiter = false; 
                Integer compressedLength = dis.readInt();
                Integer uncompressedLength = dis.readInt();
                Integer headerLength = dis.readInt();
                map.put("compressedLength", compressedLength);
                map.put("uncompressedLength", uncompressedLength);
                map.put("headerLength", headerLength);
                // System.out.println(compressedLength);
                // System.out.println(uncompressedLength);
                // System.out.println(headerLength);
                int b;
                String s = "";
                String s1 = "";
                String header = "";
                String content = "";
                while ((b=bf.read()) != -1) {
                    if (b == 0) {
                        delimiter = true;
                        continue;
                    }
                    
                    s1 = Integer.toBinaryString(b);
                    // System.out.println(s1);
                    if(s1.length()%8 > 0){
                        s1 = ("00000000".substring(s1.length()) + s1);
                        // System.out.println(s1);
                    }    
                    s += s1;
                    if(delimiter == false)
                        header += s1;
                    
                    else
                        content += s1;
                        
                }
                map.put("header", header);
                map.put("content", content);
            // System.out.println(s);
        } 
        catch (IOException e) {
            System.err.println("problem with reading");
        }
        return map;
    }

    static void decodeHeaderToTree(String s,HuffmanTree tree){
        for(int i=0;i<s.length();i++){
            if(s.charAt(i) == '0'){
                HuffmanInternalNode internalNode = new HuffmanInternalNode(null, null);
                tree = new HuffmanTree(internalNode);
            }
            else if(s.charAt(i) == '1'){
                
            }
        }
    }

    static String encodeText(String text,HashMap<Character,String> codes){
        String encoded = "";
        for(char c : text.toCharArray()){
            encoded += codes.get(c);
        }
        return encoded;
    }

    static String readFile(String url){
        String s = "";
        try (FileInputStream in = new FileInputStream(new File(url));
            BufferedInputStream bf = new BufferedInputStream(in)) {            
            int b;
            while((b = bf.read()) != -1) {
                // System.out.println(Integer.toBinaryString(b));
                s += Integer.toBinaryString(b & 0xFF);
            }
            s += "\n"+s.length();
        } 
        catch (Exception e) {
            // TODO: handle exception
        }
        return s;
    }

    public static void main(String[] args){
        TreeMap<Character,Integer> treeMap = new TreeMap<>();
        String url = new String("test.txt");
        String outputUrl = new String("output.bin");
        // String url = new String("c:/Users/admin/Desktop/New folder/pentesting.txt");
        treeMap = (new CompressionTool(url)).charactersOcurrences();
        for(Entry<Character,Integer> entry : treeMap.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        
        HuffmanTree tree = HuffmanTree.buildTree(treeMap); 
        // HuffmanTree.displayTree(tree);
        HashMap<Character,String> map = new HashMap<>();
        HuffmanTree.generateCodes(map, tree, "");
        
        for(Entry entry : map.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }

        System.out.println("***************reading content***************");
        String content = extractText(url);
        System.out.println(content);

        System.out.println("************encoded**************");
        String encoded = encodeText(content, map);
        System.out.println(encoded);

        System.out.println("************header**************");
        StringBuilder sb = new StringBuilder();
        ExtractHeaderFromTree(sb, tree);
        String header = sb.toString();
        System.out.println(header);

        System.out.println("**************write content inside the output file****************");
        writeHeaderAndContent(outputUrl,header,encoded,content.length(),encoded.length());

        // System.out.println("**************reading compressed file****************");
        // String s1 = readFile(outputUrl);
        // System.out.println(s1);
        System.out.println("**************test read write****************");
        // testWrite("test.bin");
        HashMap<String,Object> map1 = readCompressedFile(outputUrl);
        String extractedHeader = (String)map1.get("header");
        String extractedContent = (String)map1.get("content");;
        int headerLength = (Integer)map1.get("headerLength");
        int compressedLength = (Integer)map1.get("compressedLength");;
        int uncompressedLength = (Integer)map1.get("uncompressedLength");;
        System.out.println(extractedHeader);
        System.out.println(extractedContent);
        System.out.println(headerLength);
        System.out.println(compressedLength);
        System.out.println(uncompressedLength);
        // int value = 'h' & 0xFF;
        // System.out.println(Integer.toBinaryString(value));
        // String bits = String.format("%8s", Integer.toBinaryString(value))
        //              .replace(' ', '0');
        // System.out.println(bits);
    }
}

interface HuffmanBaseNode {
    int weight();
    boolean isLeaf();
}

class HuffmanLeafNode implements HuffmanBaseNode{
    private char c;
    private int freq;
    public HuffmanLeafNode(char c, int freq){
        this.c = c;
        this.freq = freq;
    }
    public char getChar(){
        return c;
    }
    public int weight(){
        return freq;
    }
    public boolean isLeaf(){
        return true;
    }
    @Override
    public String toString(){
        return "{Char: \'" + c + "\'; Freq: " + freq + "}";
    }
}

class HuffmanInternalNode implements HuffmanBaseNode{
    private int freq;
    private HuffmanBaseNode left;
    private HuffmanBaseNode right;
    public HuffmanInternalNode(HuffmanBaseNode left, HuffmanBaseNode right){
        this.left = left;
        this.right = right;
        this.freq = left.weight() + right.weight();
    }
    public int weight(){
        return freq;
    }
    public boolean isLeaf(){
        return false;
    }
    public HuffmanBaseNode getLeft(){
        return left;
    }
    public HuffmanBaseNode getRight(){
        return right;
    }
    @Override
    public String toString(){
        return "{Internal Node Freq: " + freq + "}";
    }
}

class HuffmanTree implements Comparable<HuffmanTree>{
    private HuffmanBaseNode root;
    public HuffmanTree(HuffmanBaseNode root){
        this.root = root;
    }
    public HuffmanTree(HuffmanBaseNode left, HuffmanBaseNode right,int weight){
        this.root = new HuffmanInternalNode((HuffmanInternalNode)left,(HuffmanInternalNode)right);
    }
    public HuffmanBaseNode getRoot(){
        return root;
    }
    public int weight(){
        return root.weight();
    }
    @Override
    public int compareTo(HuffmanTree o) {
        if(this.weight() < o.weight()) return -1;
        else if(this.weight() > o.weight()) return 1;
        else return 0;
    }

    static HuffmanTree buildTree(TreeMap<Character,Integer> map){
        PriorityQueue<HuffmanTree> queue = new PriorityQueue<>();
        // TreeSet<HuffmanTree> set = new TreeSet<>(Comparator.reverseOrder());
        HuffmanTree tree = null;
        // int c = 0;
        for(Entry<Character, Integer> entry : map.entrySet()){
            HuffmanBaseNode leaf = new HuffmanLeafNode(entry.getKey(),entry.getValue());
            HuffmanTree newtree = new HuffmanTree(leaf);
            queue.add(newtree);
            // c+=entry.getValue();
            // set.add(tree);
        }
        // System.out.println(c);
        while (queue.size() > 1) {
            // System.out.println(queue.poll());
            HuffmanTree left = queue.poll();
            HuffmanTree right = queue.poll();
            HuffmanInternalNode internal = new HuffmanInternalNode(left.getRoot(),right.getRoot());
            tree = new HuffmanTree(internal);
            queue.add(tree);
        }
        tree = queue.poll();
        // System.out.println(tree);
        return tree;
    }

    static void displayTree(HuffmanTree tree){
        if(tree == null) return;
        Queue<HuffmanTree> queue = new LinkedList<>();
        queue.add(tree);
        while (!queue.isEmpty()) {
            HuffmanTree current = queue.poll();
            if (current.getRoot().isLeaf()) {
                HuffmanLeafNode leaf = (HuffmanLeafNode)current.getRoot();
                System.out.println("(" + leaf.getChar() + "::" + leaf.weight() + ")");
            }
            else {
                System.out.println(current.weight());
                if (((HuffmanInternalNode)current.getRoot()).getLeft() != null) {
                    queue.add(new HuffmanTree(((HuffmanInternalNode)current.getRoot()).getLeft()));
                }
                if (((HuffmanInternalNode)current.getRoot()).getRight() != null) {
                    queue.add(new HuffmanTree(((HuffmanInternalNode)current.getRoot()).getRight()));
                }
            }
        }
    }

    static void generateCodes(HashMap<Character,String> map, HuffmanTree tree, String code){
        if(tree == null) return;
        if(tree.getRoot() == null) return;
        if(tree.getRoot().isLeaf()){
            map.put(((HuffmanLeafNode)tree.getRoot()).getChar(), code);
        }
        else{
            generateCodes(map, new HuffmanTree(((HuffmanInternalNode)tree.getRoot()).getLeft()), code+"0");
            generateCodes(map, new HuffmanTree(((HuffmanInternalNode)tree.getRoot()).getRight()), code+"1");
        }
    }

    @Override
    public String toString(){
        return root.toString();
    }
}
