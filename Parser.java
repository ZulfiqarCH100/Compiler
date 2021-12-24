import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parser {
    private FileReader fr = null;
    public FileWriter parsetree = null;
    private BufferedReader reader;
    int tabs = 0;
    private Token lookAhead;
    private String fix = null;

    List<String> lines = new ArrayList<String>();

    Boolean newSymbol = false;
    HashMap<String, Symbol> symbolTable = new HashMap<>();
    Symbol info = new Symbol();
    String name = null;
    int relativeAddress = 0; // Will increment according to the datatype.

    Parser() {
        try {
            File myObj = new File("parsetree.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
            parsetree = new FileWriter("parsetree.txt");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    private void print(String temp) {
        System.out.println(temp);
    }

    private void terminate(String error) {
        print(error);
        System.exit(1);
    }

    private boolean tokenEquals(String temp) {
        return lookAhead.name.equals(temp);
    }

    private Token getNextToken() {
        Token token = new Token();
        String temp = null;
        try {
            while (true) {
                temp = reader.readLine();
                if (temp == null) {
                    token.type = "end";
                    token.name = "end";
                    break;
                } else {
                    int comma = temp.indexOf(",");
                    token.type = temp.substring(1, comma);
                    token.name = temp.substring(comma + 2, temp.length() - 1);
                    if (!token.type.equals("COMMENT")) // Ignores all comment tokens.
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    private void writeToFile(String s) {
        try {
            parsetree.write(s + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CheckEnd() {
        if (tokenEquals(";")) {
            printTabs(++tabs);
            writeToFile("Operator ;");
            tabs--;
            match(";");
        } else
            terminate("; missing");
    }

    private void match(String temp) {
        if (temp.equals(lookAhead.name)) {
            lookAhead = getNextToken();
        } else {
            terminate("Bad Token!");
        }
    }

    private String Fix() {
        if (tokenEquals("++")) {
            printTabs(1 + tabs);
            writeToFile("Operator ++");
            match("++");
            return "++";
        } else if (tokenEquals("--")) {
            printTabs(1 + tabs);
            writeToFile("Operator --");
            match("--");
            return "--";
        } else {
            return null;
        } // Since fix can also be null, there is no need for an else.
    }

    private void Input() {
        printTabs(++tabs);
        writeToFile("I/O");

        if (lookAhead.name.equals("input")) {
            printTabs(++tabs);
            writeToFile("Input");
            tabs--;

            match("input");
            if (lookAhead.name.equals("->")) {
                match("->");
                printTabs(++tabs);
                writeToFile("Operator ->");
                tabs--;

                toPrint = toPrint + "IN ";
                String v = Variable();
                toPrint = toPrint + v;
                CheckEnd();

                quad(); // Print three adress code.
            } else
                terminate("input must be followed by ->");
        } else {
            terminate("input must start with INPUT token");
        }
        tabs--;
    }

    private String Print() {
        printTabs(++tabs);
        String toReturn = "";
        if (tokenEquals("print")) {
            match("print");
            toReturn = "OUT";
            writeToFile("print");
        } else if (tokenEquals("println")) {
            match("println");
            toReturn = "OUTln";
            writeToFile("println");
        } else {
            terminate("Output must start with PRINT or PRINTLN");
        }
        tabs--;
        return toReturn;
    }

    private void Output() {
        printTabs(++tabs);
        writeToFile("I/O");
        String nextLine = Print();
        toPrint = toPrint + "OUT"; // print or println
        toPrint = toPrint + " ";

        if (tokenEquals("(")) {
            printTabs(tabs + 1);
            writeToFile("Operator (");
            match("(");
        } else
            terminate("Enclose the message to be printed in ()");

        toPrint = toPrint + Message();

        if (tokenEquals(")")) {
            printTabs(tabs + 1);
            writeToFile("Operator )");
            match(")");
        } else
            terminate("Enclose the message to be printed in ()");
        CheckEnd();
        tabs--;
        quad();
        if (nextLine.equals("OUTln"))
            quad("OUT \\n");
    }

    private String Message() {
        String toReturn = "";
        // If string
        printTabs(++tabs);
        writeToFile("Message");
        if (lookAhead.type.equals("CONST") && lookAhead.name.startsWith("\"")) {
            printTabs(++tabs);
            writeToFile("CONST (" + lookAhead.name + ")");
            toReturn = lookAhead.name;
            match(lookAhead.name);
            tabs--;
        } else {
            toReturn = Arithmetic();
        }
        tabs--;
        return toReturn;
    }

    private String Variable() {
        printTabs(++tabs);
        writeToFile("Variable");
        String varName = "";

        fix = Fix();
        if (lookAhead.type.equals("IDENTIFIER")) {
            printTabs(++tabs);
            writeToFile("Identifier (" + lookAhead.name + ")");
            tabs--;

            if (newSymbol)
                name = lookAhead.name;

            varName = lookAhead.name;
            match(lookAhead.name);

            if (fix != null) {
                quad(varName + " = " + varName + fix.charAt(0) + "1");
            }
        } else {
            terminate("Identifier Expected");
        }
        String secondFix = Fix();
        if (secondFix != null)
            quad(varName + " = " + varName + secondFix.charAt(0) + "1");

        if (fix == null) { // cannot allow postfix if prefix is already true.
            fix = secondFix;
        }
        tabs--;
        return varName;
    }

    private String Relation() {
        printTabs(++tabs);
        writeToFile("Relation");
        String temp = lookAhead.name;
        if (tokenEquals("<") || tokenEquals(">") || tokenEquals("<=") || tokenEquals(">=") || tokenEquals("==")
                || tokenEquals("!=")) {
            printTabs(++tabs);
            writeToFile("Operator " + lookAhead.name);
            tabs--;
            match(temp);
        } else
            terminate("Relational operator missing or wrong");
        tabs--;
        return temp;
    }

    private String Expression() {
        printTabs(++tabs);
        writeToFile("Expression");
        String temp1 = Arithmetic();
        String relation = Relation();
        String temp2 = Arithmetic();

        tabs--;
        return (temp1 + " " + relation + " " + temp2);
    }

    private void Declaration() {
        printTabs(++tabs);
        writeToFile("Declaration");

        if (lookAhead.type.equals("DATATYPE")) {
            printTabs(++tabs);
            writeToFile("Datatype " + lookAhead.name);

            // Getting ready to insert the new symbol(s)
            newSymbol = true;

            // Type of the new symbol
            info.type = lookAhead.name;
            info.location = relativeAddress;

            match(lookAhead.name);
            tabs--;
            if (tokenEquals(":")) {
                printTabs(++tabs);
                writeToFile("Operator :");
                match(":");
                tabs--;
                Declaration1();
                CheckEnd();
            } else
                terminate(": missing");
        } else
            terminate("Datatype expected");
        tabs--;
    }

    private void Declaration1() {
        printTabs(++tabs);
        writeToFile("Declaration 1");
        Declaration2();
        Declaration3();
        tabs--;
    }

    private void updateSymbolTable() {
        symbolTable.put(name, new Symbol(info.type, info.location));
        if (info.type.equals("int"))
            relativeAddress = relativeAddress + 4;
        else
            relativeAddress++;
        newSymbol = false;
        name = null;
    }

    private void Declaration2() {
        printTabs(++tabs);
        writeToFile("Declaration 2");

        String var = Variable();

        if (name != null && info.type != null)
            if (!symbolTable.containsKey(name))
                updateSymbolTable();

        if (tokenEquals("=")) {
            match("=");
            printTabs(++tabs);
            writeToFile("Operator =");
            tabs--;
            String ass = Assignment2();
            quad(var + " = " + ass);
        }
        tabs--;
    }

    private void Declaration3() {
        printTabs(++tabs);
        writeToFile("Declaration 3");
        if (tokenEquals(",")) {
            printTabs(++tabs);
            writeToFile("Operator ,");
            match(",");
            tabs--;
            newSymbol = true; // Getting ready to add another variable of same type.
            info.location = relativeAddress;
            Declaration1();
        } else {
            printTabs(++tabs);
            writeToFile("null");
            tabs--;
        }
        tabs--;
    }

    private void Assignment1() {
        printTabs(++tabs);
        writeToFile("Assignment");
        String var = Variable();
        if (tokenEquals("=")) {
            match("=");
            printTabs(1 + tabs);
            writeToFile("Operator =");
            String rest = Assignment2();
            quad(var + " = " + rest);
        } else {
            if (fix == null)
                terminate("++ or -- expected");
            else
                fix = null;
        }
        CheckEnd();
        tabs--;
    }

    private String Assignment2() {
        // If string
        printTabs(++tabs);
        writeToFile("Assignment 2");

        String toReturn = "";

        if (lookAhead.name.startsWith("\"")) {
            printTabs(++tabs);
            writeToFile("CONST " + "(" + lookAhead.name + ")");
            toReturn = lookAhead.name;
            match(lookAhead.name);
            tabs--;
            return toReturn;
        } else if (lookAhead.type.equals("CONST") || tokenEquals("(")) {
            String temp = Arithmetic();
            tabs--;
            return temp;
        } else {
            String var = Variable();
            if (tokenEquals("=")) {
                match("=");
                printTabs(++tabs);
                writeToFile("Operator =");
                String temp = var + " = ";
                String ret = Assignment2();
                quad(temp + ret);
                tabs--;
                return var;
            } else {
                String temp = Arithmetic2(var);
                tabs--;
                return temp;
            }
        }
    }

    private String Arithmetic2(String var) {
        printTabs(++tabs);
        writeToFile("Arithmetic 2");
        String newTemp = "";
        String mul = DivMul1();
        if (mul != null) {
            newTemp = newTmp();
            quad(newTemp + " = " + var + " " + mul);
        }

        if (newTemp != "")
            var = newTemp;
        String sum = Arithmetic1();
        if (sum == null)
            return var;

        newTemp = newTmp();
        quad(newTemp + " = " + var + " " + sum);
        tabs--;
        return newTemp;
    }

    private String Arithmetic() {
        printTabs(++tabs);
        writeToFile("Arithmetic");

        String Print = "";
        String newTemp = "";
        Print = Print + DivMul();

        String temp = Arithmetic1();
        tabs--;
        if (temp == null)
            return Print;

        newTemp = newTmp();
        quad(newTemp + " = " + Print + " " + temp);
        return newTemp;
    }

    private String Arithmetic1() {
        printTabs(++tabs);
        writeToFile("Arithmetic 1");
        String toReturn = "";
        if (tokenEquals("+") || tokenEquals("-")) {
            printTabs(++tabs);
            writeToFile("Operator " + lookAhead.name);
            tabs--;
            toReturn = toReturn + lookAhead.name; // Adding the sign.
            match(lookAhead.name);
            String temp = DivMul(); // Getting the variable.
            String toCalculate = Arithmetic1(); // Recursive call.

            if (toCalculate != null) {
                String newTemp = newTmp();
                quad(newTemp + " = " + temp + " " + toCalculate); // This is like T10 = var operation var
                // Now lets return the newTemp for further operations with the sign behind it.
                return toReturn + " " + newTemp;
            } else {
                return toReturn + " " + temp; // If last then no calculaton needed.
            }

        } else {
            printTabs(++tabs);
            writeToFile("null");
            tabs--;
        }
        tabs--;
        // No else here as Arithmatic 1 can also be null
        return null;
    }

    private String DivMul() {
        printTabs(++tabs);
        writeToFile("DivMul");
        String Print = "";
        String newTemp = "";

        Print = Print + Vals();

        String temp = DivMul1();
        if (temp == null)
            return Print;

        newTemp = newTmp();
        quad(newTemp + " = " + Print + " " + temp);
        tabs--;
        return newTemp;
    }

    private String DivMul1() {
        printTabs(++tabs);
        writeToFile("DivMul 1");
        String toReturn = "";

        if (tokenEquals("*") || tokenEquals("/")) {
            printTabs(++tabs);
            writeToFile("Operator " + lookAhead.name);

            toReturn = toReturn + lookAhead.name; // Adding the sign.
            match(lookAhead.name);
            String temp = Vals(); // Getting the variable.
            String toCalculate = DivMul1(); // Recursive call.

            if (toCalculate != null) {
                String newTemp = newTmp();
                quad(newTemp + " = " + temp + " " + toCalculate); // This is like T10 = var operation var
                // Now lets return the newTemp for further operations with the sign behind it.
                tabs = tabs - 2;
                return toReturn + " " + newTemp;
            } else {
                tabs = tabs - 2;
                return toReturn + " " + temp; // If last then no calculaton needed.
            }
        } else {
            printTabs(++tabs);
            writeToFile("null");
            tabs = tabs - 2;
        }
        tabs--;
        // No else here as Arithmatic 1 can also be null
        return null;
    }

    private String Vals() {
        printTabs(++tabs);
        writeToFile("Vals");
        String toReturn = "";
        if (lookAhead.type.equals("CONST")) {
            printTabs(++tabs);
            writeToFile("CONST " + "(" + lookAhead.name + ")");
            toReturn = toReturn + lookAhead.name;
            match(lookAhead.name);
            tabs--;
            tabs--;
            return toReturn;
        } else if (tokenEquals("(")) {
            match("(");
            printTabs(++tabs);
            writeToFile("Operator (");

            toReturn = Arithmetic();

            if (tokenEquals(")")) {
                match(")");
                printTabs(++tabs);
                writeToFile("Operator )");
                tabs--;
            } else
                terminate(") missing");
            tabs--;
        } else
            toReturn = Variable();
        tabs--;
        return toReturn;
    }

    private String ELIF() {
        printTabs(++tabs);
        writeToFile("Else/Elif");
        int backpatch = 0;
        if (tokenEquals("elif")) {
            printTabs(1 + tabs);
            writeToFile("ELIF");
            match("elif");
            String exp = Expression();
            quad("ELIF" + " " + exp + " GOTO " + String.valueOf(lineCount + 2));
            backpatch = lineCount; // Saving the backpatch line index.
            quad("GOTO ");
            if (tokenEquals(":")) {
                printTabs(1 + tabs);
                writeToFile("Operator :");
                match(":");
                if (tokenEquals("{")) {
                    printTabs(1 + tabs);
                    writeToFile("Operator {");
                    match("{");
                    Code();
                    String toReplace = lines.get(backpatch);
                    toReplace = toReplace + String.valueOf(lineCount + 1);
                    lines.set(backpatch, toReplace);
                    if (tokenEquals("}")) {
                        printTabs(1 + tabs);
                        writeToFile("Operator }");
                        match("}");
                    } else
                        terminate("} expected");
                } else
                    terminate("{ expected");
            } else
                terminate(": expected");
            backpatch = lineCount;
            quad("GOTO ");
            String end = ELIF();
            String toReplace = lines.get(backpatch);
            toReplace = toReplace + end;
            lines.set(backpatch, toReplace);
        }

        else if (tokenEquals("else")) {
            printTabs(1 + tabs);
            writeToFile("ELSE");
            match("else");
            quad("ELSE" + " " + " GOTO " + String.valueOf(lineCount + 2));
            backpatch = lineCount; // Saving the backpatch line index.
            quad("GOTO ");
            if (tokenEquals(":")) {
                printTabs(1 + tabs);
                writeToFile("Operator :");
                match(":");
                if (tokenEquals("{")) {
                    printTabs(1 + tabs);
                    writeToFile("Operator {");
                    match("{");
                    Code();
                    String toReplace = lines.get(backpatch);
                    toReplace = toReplace + String.valueOf(lineCount);
                    lines.set(backpatch, toReplace);
                    if (tokenEquals("}")) {
                        printTabs(1 + tabs);
                        writeToFile("Operator }");
                        match("}");
                    } else
                        terminate("} expected");
                } else
                    terminate("{ expected");
            } else
                terminate(": expected ");
        } else {
            printTabs(1 + tabs);
            writeToFile("null");
            return String.valueOf(lineCount);
        }
        tabs--;
        return String.valueOf(lineCount);
    }

    private void Selection() {
        printTabs(++tabs);
        writeToFile("Flow");
        int backpatch = 0;
        if (tokenEquals("if")) {
            printTabs(1 + tabs);
            writeToFile("IF");
            match("if");
            String exp = Expression(); // Get the condition.
            quad("IF" + " " + exp + " GOTO " + String.valueOf(lineCount + 2));
            backpatch = lineCount; // Saving the backpatch line index.
            quad("GOTO ");
            if (tokenEquals(":")) {
                printTabs(1 + tabs);
                writeToFile("Operator :");
                match(":");
                if (tokenEquals("{")) {
                    printTabs(1 + tabs);
                    writeToFile("Operator {");
                    match("{");
                    Code();
                    // We can backpatch here!
                    String toReplace = lines.get(backpatch);
                    toReplace = toReplace + String.valueOf(lineCount + 1); // +1 to skip the last jump.
                    lines.set(backpatch, toReplace);

                    if (tokenEquals("}")) {
                        printTabs(1 + tabs);
                        writeToFile("Operator }");
                        match("}");
                    } else
                        terminate("} expected");
                } else
                    terminate("{ expected");
            } else
                terminate(": expected");
            backpatch = lineCount;
            quad("GOTO ");
            String end = ELIF();
            String toReplace = lines.get(backpatch);
            toReplace = toReplace + end;
            lines.set(backpatch, toReplace);
        } else
            terminate("if expected");
        tabs--;
    }

    private void Loop() {
        printTabs(++tabs);
        writeToFile("Loop");
        int backpatch = 0;

        if (tokenEquals("while")) {
            printTabs(1 + tabs);
            writeToFile("while");
            match("while");
            String exp = Expression();
            int condition = lineCount; // Saving where to return to to loop.
            quad("IF " + exp + " GOTO " + String.valueOf(lineCount + 2));
            backpatch = lineCount;
            quad("GOTO ");
            if (tokenEquals(":")) {
                printTabs(1 + tabs);
                writeToFile("Operator :");
                match(":");
                if (tokenEquals("{")) {
                    printTabs(1 + tabs);
                    writeToFile("Operator {");
                    match("{");
                    Code();
                    quad("GOTO " + String.valueOf(condition));
                    String toReplace = lines.get(backpatch);
                    toReplace = toReplace + String.valueOf(lineCount);
                    lines.set(backpatch, toReplace);
                    if (tokenEquals("}")) {
                        printTabs(1 + tabs);
                        writeToFile("Operator }");
                        match("}");
                    } else
                        terminate("} expected");
                } else
                    terminate("{ expected");
            } else
                terminate(": expected");
        }
        tabs--;
    }

    private void printTabs(int spaces) {
        for (int i = 2; i < spaces; i++)
            try {
                parsetree.write("\t");
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void Code1() {
        ++tabs;
        if (lookAhead.type.equals("DATATYPE")) {
            Declaration();
        } else if (tokenEquals("input")) {
            Input();
        } else if (tokenEquals("print") || tokenEquals("println")) {
            Output();
        } else if (tokenEquals("if")) {
            Selection();
        } else if (tokenEquals("while")) {
            Loop();
        } else if (tokenEquals("++") || tokenEquals("--") || lookAhead.type.equals("IDENTIFIER")) {
            Assignment1();
        } else
            terminate("Unknown Token: " + lookAhead.name);
        tabs--;
    }

    private void Code() {

        Code1();
        if (!lookAhead.name.equals("end") && !lookAhead.type.equals("end")) {
            if (tokenEquals("}"))
                return;
            Code();
        } else
            return;
    }

    private void setFileReader(String filename) {
        fr = null;
        try {
            fr = new FileReader(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Below are the Translation Scheme Functions.
    private int tempCount = 0; // Used to create new temps for operations.
    private int lineCount = 0;
    private String toPrint = "";

    private String newTmp() {
        String temp = "T" + String.valueOf(tempCount++);
        name = temp;
        newSymbol = true;
        info.type = "int";
        info.location = relativeAddress;
        updateSymbolTable();
        return (temp);
    }

    private void quad() {
        // System.out.println(lineCount + ": " + toPrint);
        lines.add((String.valueOf(lineCount) + ": " + toPrint));
        lineCount++;
        toPrint = ""; // Clearing the line to prepare for new line.
    }

    private void quad(String s) {
        // System.out.println(lineCount + ": " + s);
        lines.add((String.valueOf(lineCount) + ": " + s));
        lineCount++;
    }

    public void printSymbolTable() {
        // Write the symbol table to symboltable.txt

        try {
            // Creating output file.
            File myObj = new File("symboltable.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }

            // Writing to the file.
            FileWriter myWriter = new FileWriter("symboltable.txt");
            symbolTable.forEach((k, v) -> {
                try {
                    myWriter
                            .write(k + " " + v.type + " " + String.valueOf(v.location)
                                    + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    public void printTAC() {
        // write three adress code to tac.txt.
        try {
            // Creating output file.
            File myObj = new File("tac.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }

            // Writing to the file.
            FileWriter myWriter = new FileWriter("tac.txt");
            for (int x = 0; x < lines.size(); x++) {
                myWriter.write(lines.get(x) + "\n");
            }
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Parser myParser = new Parser();

        myParser.setFileReader("Output.txt");
        myParser.reader = new BufferedReader(myParser.fr);
        myParser.lookAhead = myParser.getNextToken();
        myParser.Code();
        System.out.println("\nCode Parsing Successful!");

        myParser.printSymbolTable();
        myParser.printTAC();
        try {
            myParser.parsetree.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
