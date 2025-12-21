import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
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
    static void writeHeader(String url,/*TreeMap<Character,Integer>*/String header){
        //File can t be declared separately inside try with ressources cuz it is not autocloseable
        try (FileOutputStream out = new FileOutputStream(new File(url));
        BufferedOutputStream bf = new BufferedOutputStream(out)){
            // for(Character c : header.keySet()){
            //     bf.write(new String(c + ":" + header.get(c)).getBytes(StandardCharsets.UTF_8));
            // }
            bf.write(header.getBytes(StandardCharsets.UTF_8));
            bf.write("0".getBytes(StandardCharsets.UTF_8));
        }
        catch(IOException e){
            System.err.println("problem with writing");
        } 
    }

    static void writeContent(String url,String content){
        try (FileOutputStream out = new FileOutputStream(new File(url));
        BufferedOutputStream bf = new BufferedOutputStream(out)) {
            String s = "";
            for(int i=0;i<content.length();i++){
                s = s+content.charAt(i);
                if (s.length()==8) {
                    //use int as temporary container to manage bits int is 32 bits but in the file just the 8 bits will be written 0s in the left will be ingnored
                    // 00000000 00000000 00000001 01101100 only the last 8 bits will be written cuz write() only write 8 bits
                    int value = 0;
                    for(int j=0;j<8;i++){
                        //left shifting for each step and use bitwise OR to make shifting by 1 or 0 depending on the char soming from s is 0 or 1
                        value = (value << 1) | (s.charAt(j)-'0');
                    }
                    s = "";
                    bf.write(value);
                }
            }
            if (!s.isEmpty()) {
                s += "00000000".substring(8-(s.length()%8));
            }
        } catch (IOException e) {
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
            sb.append("1"+leaf.getChar());
            // System.out.println(sb);
        }
        else{
            sb.append("0");
            HuffmanInternalNode internalNode = (HuffmanInternalNode)tree.getRoot();
            ExtractHeaderFromTree(sb, new HuffmanTree(internalNode.getLeft()));
            ExtractHeaderFromTree(sb, new HuffmanTree(internalNode.getRight()));
        }
    }

    static String encodeText(String text,HashMap<Character,String> codes){
        String encoded = "";
        for(char c : text.toCharArray()){
            encoded += codes.get(c);
        }
        return encoded;
    }

    public static void main(String[] args){
        TreeMap<Character,Integer> treeMap = new TreeMap<>();
        String url = new String("test.txt");
        String outputUrl = new String("output.txt");
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
        String content = CompressionTool.extractText(url);
        System.out.println(content);

        System.out.println("************encoded**************");
        String encoded = CompressionTool.encodeText(content, map);
        System.out.println(encoded);

        System.out.println("************header**************");
        StringBuilder sb = new StringBuilder();
        CompressionTool.ExtractHeaderFromTree(sb, tree);
        String header = sb.toString();
        System.out.println(header);

        System.out.println("**************write header isnide the output file****************");
        writeHeader(outputUrl, header);
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
