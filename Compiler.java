import java.io.BufferedReader;

//The main coderunner class.

public class Compiler {
    public static void main(String[] args) {
        Parser myParser = new Parser();
        VM myVm = new VM();

        // For Lexer.
        System.out.println("::LEXER::\n");

        String filename = "Input.txt";
        if (args.length == 1)
            filename = args[0];
        Lexer.fr = Lexer.getFileReader(filename);
        Lexer.fw = Lexer.getFileWriter();
        Lexer.run();
        try {
            Lexer.fr.close();
            Lexer.fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // For Parser.
        System.out.println("\n\n::PARSER::\n");
        myParser.setFileReader("Output.txt");
        myParser.reader = new BufferedReader(myParser.fr);
        myParser.lookAhead = myParser.getNextToken();
        myParser.Code();
        System.out.println("\nCode Parsing Successful!");
        myParser.printSymbolTable();
        myParser.printTAC();

        // For Virtual Machine.
        System.out.println("\n\n::VIRTUAL MACHINE::\n");
        myVm.readTAC();
        myVm.readSymbolTable();
        myVm.execute();
    }
}
