import java.io.BufferedReader;

//The main coderunner class.

public class Compiler {
    public static void main(String[] args) {

        VM myVm = new VM();

        // For Lexer.
        System.out.println(Colors.GREEN_BOLD_BRIGHT + "\n::LEXER::\n" + Colors.RESET);

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
        System.out.println(Colors.CYAN_BOLD_BRIGHT + "\n\n::PARSER::\n" + Colors.RESET);
        Parser myParser = new Parser();
        myParser.setFileReader("Output.txt");
        myParser.reader = new BufferedReader(myParser.fr);
        myParser.lookAhead = myParser.getNextToken();
        myParser.Code();
        System.out.println("\nCode Parsing Successful!");
        myParser.printSymbolTable();
        myParser.printTAC();

        // For Virtual Machine.
        System.out.println(Colors.YELLOW_BOLD_BRIGHT + "\n\n::VIRTUAL MACHINE::\n" + Colors.RESET);
        myVm.readTAC();
        myVm.readSymbolTable();
        myVm.execute();
    }
}
