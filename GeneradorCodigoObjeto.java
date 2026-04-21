import java.util.*;

public class GeneradorCodigoObjeto {
    private List<String> instrucciones = new ArrayList<>();
    private Map<String, Integer> etiquetas = new HashMap<>();
    private List<GeneradorCodigoIntermedio.Cuadrupla> cuadruplas;

    public GeneradorCodigoObjeto(List<GeneradorCodigoIntermedio.Cuadrupla> cuadruplas) {
        this.cuadruplas = cuadruplas;
    }

    public void generar() {
        // Primera pasada: resolver etiquetas
        int lineaCodigo = 0;
        for (GeneradorCodigoIntermedio.Cuadrupla c : cuadruplas) {
            if (c.operador.equals("LABEL")) {
                etiquetas.put(c.resultado, lineaCodigo);
            } else {
                lineaCodigo += contarInstrucciones(c);
            }
        }

        // Segunda pasada: generar instrucciones
        for (GeneradorCodigoIntermedio.Cuadrupla c : cuadruplas) {
            traducir(c);
        }
    }

    private int contarInstrucciones(GeneradorCodigoIntermedio.Cuadrupla c) {
        String op = c.operador;
        switch (op) {
            case "DECL": return 1;
            case "=": return 2;
            case "PRINT": return 2;
            case "READ": return 1;
            case "IF_FALSE": return 2;  // PUSH + JZ
            case "IF_TRUE": return 2;   // PUSH + JNZ
            case "GOTO": return 1;
            case "LABEL": return 0;
            default: // operaciones binarias: PUSH + PUSH + OP + STORE = 4
                if (c.arg2 != null) return 4;
                return 3; // unarias: PUSH + OP + STORE
        }
    }

    private void traducir(GeneradorCodigoIntermedio.Cuadrupla c) {
        String op = c.operador;
        switch (op) {
            case "DECL":
                emitir("DECL_VAR " + c.resultado + " " + c.arg1);
                break;

            case "=":
                emitir("PUSH " + c.arg1);
                emitir("STORE " + c.resultado);
                break;

            case "+": case "-": case "*": case "/":
            case "==": case "!=": case "<": case ">":
            case "<=": case ">=": case "&&": case "||":
                emitir("PUSH " + c.arg1);
                emitir("PUSH " + c.arg2);
                String instrOp;
                switch (op) {
                    case "+": instrOp = "ADD"; break;
                    case "-": instrOp = "SUB"; break;
                    case "*": instrOp = "MUL"; break;
                    case "/": instrOp = "DIV"; break;
                    case "==": instrOp = "CMP_EQ"; break;
                    case "!=": instrOp = "CMP_NE"; break;
                    case "<": instrOp = "CMP_LT"; break;
                    case ">": instrOp = "CMP_GT"; break;
                    case "<=": instrOp = "CMP_LE"; break;
                    case ">=": instrOp = "CMP_GE"; break;
                    case "&&": instrOp = "AND"; break;
                    case "||": instrOp = "OR"; break;
                    default: instrOp = "NOP"; break;
                }
                emitir(instrOp);
                emitir("STORE " + c.resultado);
                break;

            case "PRINT":
                emitir("PUSH " + c.arg1);
                emitir("OUT");
                break;

            case "READ":
                emitir("IN " + c.resultado);
                break;

            case "IF_FALSE":
                emitir("PUSH " + c.arg1);
                emitir("JZ " + resolverEtiqueta(c.resultado));
                break;

            case "IF_TRUE":
                emitir("PUSH " + c.arg1);
                emitir("JNZ " + resolverEtiqueta(c.resultado));
                break;

            case "GOTO":
                emitir("JMP " + resolverEtiqueta(c.resultado));
                break;

            case "LABEL":
                // No emitir nada, las etiquetas ya se resolvieron
                break;

            default:
                if (c.arg2 == null && c.resultado != null) {
                    // Unario
                    emitir("PUSH " + c.arg1);
                    emitir("NEG");
                    emitir("STORE " + c.resultado);
                }
                break;
        }
    }

    private String resolverEtiqueta(String etiqueta) {
        if (etiquetas.containsKey(etiqueta)) {
            return String.valueOf(etiquetas.get(etiqueta));
        }
        return etiqueta;
    }

    private void emitir(String instruccion) {
        instrucciones.add(instruccion);
    }

    public void mostrarCodigoObjeto() {
        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.println("║             CÓDIGO OBJETO GENERADO            ║");
        System.out.println("╠═══════╦═══════════════════════════════════════╣");
        System.out.printf("║ %-5s ║ %-37s ║\n", "Dir", "Instrucción");
        System.out.println("╠═══════╬═══════════════════════════════════════╣");
        for (int i = 0; i < instrucciones.size(); i++) {
            String instr = instrucciones.get(i);
            if (instr.length() > 37) instr = instr.substring(0, 37);
            System.out.printf("║ %-5d ║ %-37s ║\n", i, instr);
        }
        System.out.println("╚═══════╩═══════════════════════════════════════╝");
    }

    public List<String> getInstrucciones() { return instrucciones; }
}
