import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeMap;

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
                    map.put(ch,0);
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
        System.out.println(treeMap);
    }
}

class Tree{
    byte val;
    Tree left;
    Tree right;
}