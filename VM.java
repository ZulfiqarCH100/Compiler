import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class VM {
    private FileReader fr = null;
    private BufferedReader reader;

    // Stores the lines of three adress code to execute.
    List<String> lines = new ArrayList<String>();
    int currentLine = 0;
    // stores the symbol table.
    HashMap<String, Symbol> symbolTable = new HashMap<>();
    // Byte array to store char and int values of all variables. We have relative
    // adresses in symbol table to help traverse this.
    byte[] data = null;
    int dataSize = 0;

    boolean[] defined = null; // Contains info about a variable's value being defined or not.

    private class Calculation {
        String varA;
        String varB;
        String operation;
    };

    private void print(String temp) {
        System.out.println(temp);
    }

    private void terminate(String error) {
        System.out.println(Colors.RED_BOLD + error + Colors.RESET);
        System.exit(1);
    }

    // Gets the type of a variable.
    private String getType(String name) {
        return symbolTable.get(name).type;
    }

    // Converts an int into an array of 4 bytes to store in the data array.
    public void storeInt(String name, int value) {
        int address = symbolTable.get(name).location;
        defined[address] = true;
        byte[] temp = new byte[4];
        temp = ByteBuffer.allocate(4).putInt(value).array();
        for (int i = 0; i < 4; i++) {
            data[i + address] = temp[i];
        }

    }

    // Gets an integer from the byte array at a relative address.
    public int getInt(String name) {
        int address = symbolTable.get(name).location;
        byte[] toConvert = new byte[4]; // Temp array to store the extracted 4 bytes.

        if (defined[address] == true) {
            for (int i = 0; i < 4; i++) {
                toConvert[i] = data[address + i];
            }

            ByteBuffer bb = ByteBuffer.wrap(toConvert);
            return bb.getInt();
        } else
            terminate("Variable " + name + " is accessed but it's value has not been initalized");
        return -1;
    }

    public char getChar(String name) {
        int address = symbolTable.get(name).location;
        if (defined[address] == true)
            return (char) data[address];
        else
            terminate("Variable " + name + " is accessed but it's value has not been initalized");
        return 0;
    }

    public void storeChar(String name, char temp) {
        int address = symbolTable.get(name).location;
        defined[address] = true;
        byte toStore = (byte) temp;
        data[address] = toStore;
    }

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
    public void readTAC() {
        setFileReader("tac.txt");
        reader = new BufferedReader(fr);
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
        lines.add("end"); // indicator that the code is finished.
    }

    public void readSymbolTable() {
        setFileReader("symboltable.txt");
        reader = new BufferedReader(fr);
        int max = -1;
        String maxType = null;

        while (true) {
            try {
                String temp = reader.readLine(); // Reading a line.

                // Removing the line number and putting into list.
                if (temp == null)
                    break;
                else {
                    int firstSpace = temp.indexOf(" ");
                    String substring = temp.substring(firstSpace + 1, temp.length());
                    int secondSpace = substring.indexOf(" ");

                    // Reading symbols
                    String name = temp.substring(0, firstSpace);
                    String type = substring.substring(0, secondSpace);
                    int address = Integer.parseInt(substring.substring(secondSpace + 1, substring.length()));

                    if (address > max) {
                        max = address;
                        maxType = type;
                    }

                    Symbol s = new Symbol(type, address);
                    symbolTable.put(name, s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (max >= 0) {
            // Allocating space in data segment.
            if (maxType.equals("char"))
                dataSize = max + 1;
            else
                dataSize = max + 4;

            data = new byte[dataSize];
            defined = new boolean[dataSize];

            for (int i = 0; i < dataSize; i++) {
                defined[i] = false;
            }
        }
    }

    public void debug() {
        print("The code is:");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println(lines.get(i));
        }

        print("\n\n\nThe symbol table is: ");
        symbolTable.forEach((k, v) -> {
            print(k + " " + v.type + " " + String.valueOf(v.location)
                    + "\n");
        });
    }

    private void OUT(String rest) {
        int comma = rest.indexOf("\"");
        if (comma != -1)
            // If we have to print a constant string
            System.out.print(rest.substring(comma + 1, rest.length() - 1));
        else if (rest.contains("\\n")) {
            // if we have to print a newline
            print("");
        } else {
            // if we have to print value of a variable.
            Symbol var = symbolTable.get(rest);
            if (var.type.equals("int")) {
                int temp = getInt(rest);
                System.out.print(temp);
            } else if (var.type.equals("char")) {
                char temp = getChar(rest);
                System.out.print(temp);
            }
        }

    }

    public void IN(String rest) {
        String type = symbolTable.get(rest).type;
        if (type.equals("int")) {
            Scanner in = new Scanner(System.in);
            try {
                int read = in.nextInt();
                storeInt(rest, read);
            } catch (Exception e) {
                terminate("Wrong input given to int variable, Terminating");
            }
            in.close();
        }
        if (type.equals("char")) {
            Scanner in = new Scanner(System.in);
            String read = in.nextLine();
            if (read.length() != 1)
                storeChar(rest, read.charAt(0));
            else
                terminate("Invalid input to char, Terminating");
            in.close();
        }

    }

    private int GOTO(String rest) {
        return Integer.parseInt(rest);
    }

    private void assignment(String rest, String leftSideVar) {
        int equals = rest.indexOf("=");
        rest = rest.substring(equals + 2, rest.length());
        int space = rest.indexOf(" ");

        if (space == -1) {
            int negative = 1;
            // Checking for negative.
            if (rest.charAt(0) == '-') {
                rest = rest.substring(1);
                negative = -1;
            }

            // Just assignment.
            if (rest.matches("[0-9]+")) {
                // if right side is a constant int.
                if (symbolTable.get(leftSideVar).type.equals("int")) {
                    storeInt(leftSideVar, Integer.parseInt(rest) * negative); // Can also handle negative values.
                } else
                    terminate("Cannot store int into a non int varible " + leftSideVar);
            } else if (rest.charAt(0) == '\'') {
                // if constant char.
                rest = rest.substring(1);
                int comma = rest.indexOf('\'');
                String c = rest.substring(0, comma);
                if (c.length() != 1) {
                    terminate("Char variable " + leftSideVar + " cannot store strings");
                } else if (symbolTable.get(leftSideVar).type.equals("char")) {
                    storeChar(leftSideVar, rest.charAt(0));
                } else
                    terminate("Cannot store char value in non char variable " + leftSideVar);
            } else {
                // Another variable.
                String type = symbolTable.get(leftSideVar).type;
                if (!type.equals(symbolTable.get(rest).type))
                    terminate("Type mismatch on line " + String.valueOf(currentLine));
                else if (type.equals("int")) {
                    storeInt(leftSideVar, getInt(rest) * negative);

                } else if (type.equals("char"))
                    storeChar(leftSideVar, getChar(rest));
            }

        } else {
            // Doing algebric expression.
            if (getType(leftSideVar).equals("int")) {
                int rightSide = algebra(calculation(rest));
                storeInt(leftSideVar, rightSide);
            } else {
                terminate("Cannot store result of arithmatic into a char variable. Line no: "
                        + String.valueOf(currentLine));
            }
        }
    }

    // This funcition parses the three adress code in order to make calculation and
    // comparisons easy to do.
    private Calculation calculation(String expression) {
        Calculation cal = new Calculation();
        int space = expression.indexOf(" ");
        cal.varA = expression.substring(0, space);
        expression = expression.substring(space + 1);
        space = expression.indexOf(" ");
        cal.operation = expression.substring(0, space);

        expression = expression.substring(space + 1);
        cal.varB = expression.substring(0, expression.length());

        return cal;
    }

    // Gets the values to be used in arithmatic operations.
    private int getValue(String var) {
        int negative = 1;
        // Checking for negative.
        if (var.charAt(0) == '-') {
            var = var.substring(1);
            negative = -1;
        }

        if (var.matches("[0-9]+")) {
            return Integer.parseInt(var) * negative;
        } else if (symbolTable.get(var).type.equals("int")) {
            return getInt(var) * negative;
        } else {
            terminate("Line " + String.valueOf(currentLine)
                    + ": Arithmatic operations can only be done on intgers for now, Terminating");
        }
        return -1;
    }

    private int algebra(Calculation cal) {
        int valueA = getValue(cal.varA);
        int valueB = getValue(cal.varB);

        if (cal.operation.equals("+")) {
            return valueA + valueB;
        } else if (cal.operation.equals("-")) {
            return valueA - valueB;
        } else if (cal.operation.equals("*")) {
            return valueA * valueB;
        } else if (cal.operation.equals("/")) {
            return valueA / valueB;

        }
        terminate("unidentified symbol. Line no: " + String.valueOf(currentLine));
        return 0;
    }

    private boolean expression(Calculation cal) {
        int valueA = getValue(cal.varA);
        int valueB = getValue(cal.varB);

        if (cal.operation.equals(">")) {
            return valueA > valueB;
        } else if (cal.operation.equals("<")) {
            return valueA < valueB;
        } else if (cal.operation.equals("==")) {
            return valueA == valueB;
        } else if (cal.operation.equals(">=")) {
            return valueA >= valueB;
        } else if (cal.operation.equals("<=")) {
            return valueA <= valueB;
        } else if (cal.operation.equals("!=")) {
            return valueA != valueB;
        }
        terminate("unidentified symbol. Line no: " + String.valueOf(currentLine));
        return false;
    }

    private int IF(String rest) {
        String exp = rest.substring(0, rest.indexOf("G") - 1);
        rest = rest.substring(rest.indexOf("T") + 3);
        int ifTrue = Integer.parseInt(rest);

        boolean result = expression(calculation(exp));
        if (result)
            return ifTrue;
        return currentLine + 1;
    }

    // Execute the TAC.
    public void execute() {
        while (true) {
            String line = lines.get(currentLine);
            int space = line.indexOf(" ");
            String command = null;
            String rest = null;

            if (space != -1) {
                command = line.substring(0, space);
                rest = line.substring(space + 1, line.length());
            } else
                command = "end";

            if (command.equals("OUT")) {
                OUT(rest);
                currentLine++;
            } else if (command.equals("end"))
                terminate(Colors.GREEN_BOLD + "Code execution completed successfully" + Colors.RESET);
            else if (command.equals("IN")) {
                IN(rest);
                currentLine++;
            } else if (command.equals("GOTO")) {
                currentLine = GOTO(rest);
            } else if (command.equals("IF")) {
                currentLine = IF(rest);
            } else {
                assignment(rest, command);
                currentLine++;
            }
        }
    }

    public static void main(String[] args) {
        VM vm = new VM();
        vm.readTAC();
        vm.readSymbolTable();
        vm.execute();
    }
}
