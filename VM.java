import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VM {
    private FileReader fr = null;
    private BufferedReader reader;

    List<String> lines = new ArrayList<String>();

    public void setFileReader(String filename) {
        fr = null;
        try {
            fr = new FileReader(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // We need to read the whole file once into an array so we can jump between line
    // numbers.
    public void readFile() {
        while (true) {
            try {
                String temp = reader.readLine(); // Reading a line.

                // Removing the line number and putting into list.
                if (temp == null)
                    break;
                else {
                    int colon = temp.indexOf(":");
                    lines.add(temp.substring(colon + 2, temp.length()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void debug() {
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(lines.get(i));
        }
    }

    public static void main(String[] args) {
        VM vm = new VM();
        vm.setFileReader("tac.txt");
        vm.reader = new BufferedReader(vm.fr);
        vm.readFile();
        vm.debug();
    }
}
