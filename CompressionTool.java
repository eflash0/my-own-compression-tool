import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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
    public static void main(String[] args){
        TreeMap<Character,Integer> treeMap = new TreeMap<>();
        String url = new String("c:/Users/admin/Desktop/New folder/pentesting.txt");
        treeMap = (new CompressionTool(url)).charactersOcurrences();
        // System.out.println(treeMap);
        HuffmanTree.buildTree(treeMap);
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
        for(Entry<Character, Integer> entry : map.entrySet()){
            HuffmanBaseNode leaf = new HuffmanLeafNode(entry.getKey(),entry.getValue());
            HuffmanTree newtree = new HuffmanTree(leaf);
            queue.add(newtree);
            // set.add(tree);
        }
        while (!queue.isEmpty()) {
            System.out.println(queue.poll());
            HuffmanTree left = queue.poll();
            HuffmanTree right = queue.poll();
            HuffmanBaseNode leftNode = null;
            HuffmanBaseNode rightNode = null;
            if(left != null) leftNode = left.getRoot();
            if(right != null) rightNode = right.getRoot();
            HuffmanInternalNode internal = new HuffmanInternalNode(leftNode,rightNode);
            tree = new HuffmanTree(internal);
            queue.add(tree);
        }
        System.out.println("Final tree: " + tree);
        return tree;
    }

    @Override
    public String toString(){
        return root.toString();
    }
}
