import java.util.*;

public class MaquinaVirtual {
    private List<String> instrucciones;
    private Map<String, Object> memoria = new LinkedHashMap<>();
    private Stack<Object> pila = new Stack<>();
    private int pc = 0; // program counter
    private List<String> salida = new ArrayList<>();
    private Scanner scanner;
    private Map<String, String> tiposVariables = new HashMap<>();

    public MaquinaVirtual(List<String> instrucciones) {
        this.instrucciones = instrucciones;
        this.scanner = new Scanner(System.in);
    }

    public void ejecutar() {
        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.println("║          EJECUCIÓN DEL CÓDIGO OBJETO          ║");
        System.out.println("╚═══════════════════════════════════════════════╝");
        System.out.println();

        while (pc < instrucciones.size()) {
            String instruccion = instrucciones.get(pc);
            String[] partes = instruccion.split(" ", 2);
            String opcode = partes[0];
            String operando = partes.length > 1 ? partes[1] : null;

            try {
                switch (opcode) {
                    case "DECL_VAR":
                        ejecutarDeclVar(operando);
                        break;
                    case "PUSH":
                        ejecutarPush(operando);
                        break;
                    case "STORE":
                        ejecutarStore(operando);
                        break;
                    case "ADD": ejecutarAritmetica("+"); break;
                    case "SUB": ejecutarAritmetica("-"); break;
                    case "MUL": ejecutarAritmetica("*"); break;
                    case "DIV": ejecutarAritmetica("/"); break;
                    case "NEG": ejecutarNeg(); break;
                    case "CMP_EQ": ejecutarComparacion("=="); break;
                    case "CMP_NE": ejecutarComparacion("!="); break;
                    case "CMP_LT": ejecutarComparacion("<"); break;
                    case "CMP_GT": ejecutarComparacion(">"); break;
                    case "CMP_LE": ejecutarComparacion("<="); break;
                    case "CMP_GE": ejecutarComparacion(">="); break;
                    case "AND": ejecutarLogico("&&"); break;
                    case "OR": ejecutarLogico("||"); break;
                    case "OUT":
                        ejecutarOut();
                        break;
                    case "IN":
                        ejecutarIn(operando);
                        break;
                    case "JMP":
                        pc = Integer.parseInt(operando);
                        continue;
                    case "JZ":
                        ejecutarJZ(operando);
                        continue;
                    case "JNZ":
                        ejecutarJNZ(operando);
                        continue;
                    case "NOP":
                        break;
                    default:
                        System.err.println("  [VM] Instrucción desconocida: " + opcode);
                }
            } catch (Exception e) {
                System.err.println("  [VM] Error en instrucción " + pc + ": " + e.getMessage());
            }
            pc++;
        }

        System.out.println("\n── Fin de ejecución ──");
    }

    private void ejecutarDeclVar(String operando) {
        String[] partes = operando.split(" ", 2);
        String nombre = partes[0];
        String tipo = partes.length > 1 ? partes[1] : "entero";
        tiposVariables.put(nombre, tipo);

        switch (tipo) {
            case "entero": memoria.put(nombre, 0); break;
            case "decimal": memoria.put(nombre, 0.0); break;
            case "cadena": memoria.put(nombre, ""); break;
            case "booleano": memoria.put(nombre, false); break;
            default: memoria.put(nombre, 0); break;
        }
    }

    private void ejecutarPush(String operando) {
        if (operando == null) { pila.push(0); return; }

        // Es un literal de cadena
        if (operando.startsWith("\"") && operando.endsWith("\"")) {
            pila.push(operando.substring(1, operando.length() - 1));
            return;
        }

        // Es una variable
        if (memoria.containsKey(operando)) {
            pila.push(memoria.get(operando));
            return;
        }

        // Es un número
        try {
            if (operando.contains(".")) {
                pila.push(Double.parseDouble(operando));
            } else {
                pila.push(Integer.parseInt(operando));
            }
            return;
        } catch (NumberFormatException e) {}

        // Booleano
        if (operando.equals("1") || operando.equals("verdadero")) { pila.push(true); return; }
        if (operando.equals("0") || operando.equals("falso")) { pila.push(false); return; }

        // Variable temporal no encontrada - por defecto 0
        pila.push(0);
    }

    private void ejecutarStore(String variable) {
        if (!pila.isEmpty()) {
            Object valor = pila.pop();
            // Conversión de tipo si es necesario
            String tipo = tiposVariables.get(variable);
            if (tipo != null) {
                valor = convertirTipo(valor, tipo);
            }
            memoria.put(variable, valor);
        }
    }

    private Object convertirTipo(Object valor, String tipo) {
        switch (tipo) {
            case "entero":
                if (valor instanceof Double) return ((Double) valor).intValue();
                if (valor instanceof String) {
                    try { return Integer.parseInt((String) valor); } catch (Exception e) { return 0; }
                }
                if (valor instanceof Boolean) return (Boolean) valor ? 1 : 0;
                return valor;
            case "decimal":
                if (valor instanceof Integer) return ((Integer) valor).doubleValue();
                if (valor instanceof String) {
                    try { return Double.parseDouble((String) valor); } catch (Exception e) { return 0.0; }
                }
                return valor;
            case "cadena":
                return String.valueOf(valor);
            case "booleano":
                if (valor instanceof Integer) return ((Integer) valor) != 0;
                if (valor instanceof Double) return ((Double) valor) != 0.0;
                return valor;
        }
        return valor;
    }

    private double toDouble(Object v) {
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Double) return (Double) v;
        if (v instanceof Boolean) return (Boolean) v ? 1.0 : 0.0;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }

    private boolean isInteger(Object v) {
        return v instanceof Integer;
    }

    private void ejecutarAritmetica(String op) {
        Object b = pila.pop();
        Object a = pila.pop();

        // Concatenación de cadenas
        if (op.equals("+") && (a instanceof String || b instanceof String)) {
            pila.push(String.valueOf(a) + String.valueOf(b));
            return;
        }

        double da = toDouble(a);
        double db = toDouble(b);
        double resultado;

        switch (op) {
            case "+": resultado = da + db; break;
            case "-": resultado = da - db; break;
            case "*": resultado = da * db; break;
            case "/":
                if (db == 0) {
                    System.err.println("  [VM] Error: División entre cero");
                    resultado = 0;
                } else {
                    resultado = da / db;
                }
                break;
            default: resultado = 0;
        }

        if (isInteger(a) && isInteger(b) && !op.equals("/")) {
            pila.push((int) resultado);
        } else if (isInteger(a) && isInteger(b) && op.equals("/") && resultado == Math.floor(resultado)) {
            pila.push((int) resultado);
        } else {
            pila.push(resultado);
        }
    }

    private void ejecutarNeg() {
        Object a = pila.pop();
        if (a instanceof Integer) pila.push(-(Integer) a);
        else if (a instanceof Double) pila.push(-(Double) a);
    }

    private void ejecutarComparacion(String op) {
        Object b = pila.pop();
        Object a = pila.pop();
        double da = toDouble(a);
        double db = toDouble(b);
        boolean resultado;

        switch (op) {
            case "==": resultado = da == db; break;
            case "!=": resultado = da != db; break;
            case "<": resultado = da < db; break;
            case ">": resultado = da > db; break;
            case "<=": resultado = da <= db; break;
            case ">=": resultado = da >= db; break;
            default: resultado = false;
        }

        pila.push(resultado ? 1 : 0);
    }

    private void ejecutarLogico(String op) {
        Object b = pila.pop();
        Object a = pila.pop();
        boolean ba = toDouble(a) != 0;
        boolean bb = toDouble(b) != 0;
        boolean resultado;

        switch (op) {
            case "&&": resultado = ba && bb; break;
            case "||": resultado = ba || bb; break;
            default: resultado = false;
        }

        pila.push(resultado ? 1 : 0);
    }

    private void ejecutarOut() {
        Object valor = pila.pop();
        String texto;
        if (valor instanceof Double) {
            double d = (Double) valor;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                texto = String.valueOf((int) d);
            } else {
                texto = String.valueOf(d);
            }
        } else {
            texto = String.valueOf(valor);
        }
        System.out.println("  >> " + texto);
        salida.add(texto);
    }

    private void ejecutarIn(String variable) {
        String tipo = tiposVariables.getOrDefault(variable, "cadena");
        System.out.print("  << Ingrese valor para '" + variable + "' (" + tipo + "): ");

        String entrada = scanner.nextLine().trim();
        Object valor;

        switch (tipo) {
            case "entero":
                try { valor = Integer.parseInt(entrada); }
                catch (Exception e) { valor = 0; System.err.println("  [VM] Valor inválido, usando 0"); }
                break;
            case "decimal":
                try { valor = Double.parseDouble(entrada); }
                catch (Exception e) { valor = 0.0; System.err.println("  [VM] Valor inválido, usando 0.0"); }
                break;
            default:
                valor = entrada;
                break;
        }

        memoria.put(variable, valor);
    }

    private void ejecutarJZ(String destino) {
        Object valor = pila.pop();
        double d = toDouble(valor);
        if (d == 0) {
            pc = Integer.parseInt(destino);
        } else {
            pc++;
        }
    }

    private void ejecutarJNZ(String destino) {
        Object valor = pila.pop();
        double d = toDouble(valor);
        if (d != 0) {
            pc = Integer.parseInt(destino);
        } else {
            pc++;
        }
    }

    public List<String> getSalida() { return salida; }
}
