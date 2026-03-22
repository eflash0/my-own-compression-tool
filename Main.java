import java.util.HashMap;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args){
        TreeMap<Character,Integer> treeMap = new TreeMap<>();
        String url = new String("c:/Users/admin/Desktop/New folder/pentesting.txt");
        treeMap = (new CompressionTool(url)).charactersOcurrences();
        // for(Entry<Character,Integer> entry : treeMap.entrySet()){
        //     System.out.println(entry.getKey() + ":" + entry.getValue());
        // }
        
        HuffmanTree tree = HuffmanTree.buildTree(treeMap); 
        HuffmanTree.displayTree(tree);
        String code = "";
        HashMap<Character,String> map = new HashMap<>();
        HuffmanTree.generateCodes(map, tree, code);
        
        // for(Entry entry : map.entrySet()){
        //     System.out.println(entry.getKey() + ":" + entry.getValue());
        // }
    }
}    
