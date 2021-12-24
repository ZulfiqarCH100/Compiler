import java.io.*;

public class Lexer {
    static FileReader fr;
    static FileWriter fw;
    static String buffer = "";
    static char temp = 0;

    static FileReader getFileReader(String filename) {
        FileReader fr = null;
        try {
            fr = new FileReader(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fr;
    }

    static FileWriter getFileWriter() {
        FileWriter fw = null;
        try {
            fw = new FileWriter("Output.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fw;
    }

    static boolean isIdentifier(String s) {
        if (s.length() == 0 || !Character.isAlphabetic(s.charAt(0)))
            return false;
        for (char c : s.toCharArray())
            if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_')
                return false;
        return !isDatatype(s) && !isFlow(s) && !isIO(s);
    }

    static boolean isArithmetic(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/") || s.equals("++") || s.equals("--");
    }

    static boolean isRelation(String s) {
        return s.equals("<") || s.equals("<=") || s.equals(">") || s.equals(">=") || s.equals("==") || s.equals("~=");
    }

    static boolean isConst(String s) {
        if (s.length() == 0)
            return false;

        if (Character.isDigit(s.charAt(0))) {
            for (char c : s.toCharArray())
                if (!Character.isDigit(c))
                    return false;
            return true;
        }

        if (s.startsWith("'") && s.endsWith("'") && s.length() == 3)
            return true;

        return s.startsWith("\"") && s.endsWith("\"") && s.length() > 1;
    }

    static boolean isOperator(String s) {
        return s.equals("=") || s.equals("->") || s.equals(";") || s.equals(":") || s.equals(",") || s.equals("(")
                || s.equals(")") || s.equals("{") || s.equals("}") || s.equals("[") || s.equals("]");
    }

    static boolean isDatatype(String s) {
        return s.equals("int") || s.equals("char");
    }

    static boolean isFlow(String s) {
        return s.equals("if") || s.equals("elif") || s.equals("else") || s.equals("while");
    }

    static boolean isIO(String s) {
        return s.equals("input") || s.equals("print") || s.equals("println");
    }

    static boolean isComment(String s) {
        if (s.startsWith("//"))
            return !s.contains("\r") && !s.contains("\n");

        return s.startsWith("/*") && s.endsWith("*/") && s.length() > 3;
    }

    static void printOutput(String token, int n) {
        String s = "";
        switch (n) {
            case 1 -> s = "IDENTIFIER";
            case 2 -> s = "ARITHMETIC";
            case 3 -> s = "RELATION";
            case 4 -> s = "CONST";
            case 5 -> s = "OPERATOR";
            case 6 -> s = "DATATYPE";
            case 7 -> s = "FLOW";
            case 8 -> s = "IO";
            case 9 -> s = "COMMENT";
        }
        if (n == 9)
            token = "useless comment";
        s = "(" + s + ", " + token + ")";
        System.out.println(s);

        writeToFile(s);
    }

    static void printError(String error, int line) {
        System.out.println(error + " in line number " + line);
        writeToFile(error + " in line number " + line);
    }

    static char readFromFile() {
        if (temp != 0) {
            char c = temp;
            temp = 0;
            return c;
        }

        int n = 0;
        try {
            n = fr.read();
            if (n < 0)
                n = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (char) n;
    }

    static void writeToFile(String s) {
        try {
            fw.write(s);
            fw.write('\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void popBack() {
        temp = buffer.charAt(buffer.length() - 1);
        buffer = buffer.substring(0, buffer.length() - 1);
    }

    static int runs(String s) {
        if (isIdentifier(s))
            return 1;
        if (isArithmetic(s))
            return 2;
        if (isRelation(s))
            return 3;
        if (isConst(s))
            return 4;
        if (isOperator(s))
            return 5;
        if (isDatatype(s))
            return 6;
        if (isFlow(s))
            return 7;
        if (isIO(s))
            return 8;
        if (isComment(s))
            return 9;
        return 0;
    }

    static void run() {
        int token = 0, n;
        int state = 0;
        int line = 1;
        char c;
        do {
            c = readFromFile();

            if (buffer.isEmpty() && Character.isWhitespace(c))
                continue;

            buffer += c; // Appending c to the buffer

            switch (buffer) {
                case "'" -> state = 1;
                case "\"" -> state = 2;
                case "/*" -> state = 3;
            }

            if (state == 1) { // LC
                if (!buffer.endsWith("'") || buffer.length() < 3) {
                    if (buffer.length() > 2 && c == 0) {
                        printError("Error in Literal constant", line);
                        break;
                    } else
                        continue;
                }
                state = 0;
            } else if (state == 2) { // STR
                if (!buffer.endsWith("\"") || buffer.length() < 2 || buffer.contains("\n")) {
                    if (c == 0) {
                        printError("Error in String", line);
                        break;
                    } else
                        continue;
                }
                state = 0;
            } else if (state == 3) { // MLC
                if (!buffer.endsWith("*/") || buffer.length() < 4) {
                    if (c == 0) {
                        printError("Error in Multi line comment", line);
                        break;
                    } else
                        continue;
                }
                state = 0;
            }

            if ((n = runs(buffer)) != 0)
                token = n;
            if (c == 0)
                n = 0;

            if (c == '\n' || c == 0) {
                if (n == 0 && token == 0 && !buffer.isEmpty()) {
                    printError("Unknown token", line);
                    break;
                } else
                    ++line;
            }

            if (n == 0 && token != 0) {
                popBack();
                printOutput(buffer, token);
                buffer = "";
                token = 0;
            }
        } while (c != 0);
    }

    public static void main(String[] args) {
        String filename = "Input.txt";
        if (args.length == 1)
            filename = args[0];
        fr = getFileReader(filename);
        fw = getFileWriter();

        run();

        try {
            fr.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
