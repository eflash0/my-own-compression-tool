import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

public class CompressionTool{
    private String fileUrl;
    public CompressionTool(String fileUrl){
        this.fileUrl = fileUrl;
    }
    public TreeMap<Character,Integer> charactersOcurrences(){
        try (File file = new File(fileUrl); FileInputStream in = new FileInputStream(file);
        BufferedInputStream bf = new BufferedInputStream(file)) {
            while (bf.rea d() != -1) {
                
            }

            
        } catch (FileNotFoundException e) {
            System.err.println("file not found",e.getMessage());
        }
        catch(IOException e){
            System.err.println("error while reading the file",e.getMessage());
        }
    }
    public static void main(String[] args){

    }
}

class Tree{
    byte val;
    Tree left;
    Tree right;
}