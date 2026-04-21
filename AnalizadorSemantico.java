import java.util.*;

public class AnalizadorSemantico {
    // Tabla de símbolos semántica: nombre -> tipo de dato
    private Map<String, String> tablaVariables = new LinkedHashMap<>();
    private List<String> errores = new ArrayList<>();
    private List<String> advertencias = new ArrayList<>();

    public void analizar(NodoPrograma programa) {
        for (NodoAST sentencia : programa.sentencias) {
            analizarNodo(sentencia);
        }
    }

    private void analizarNodo(NodoAST nodo) {
        if (nodo instanceof NodoDeclVariable) analizarDeclaracion((NodoDeclVariable) nodo);
        else if (nodo instanceof NodoAsignacion) analizarAsignacion((NodoAsignacion) nodo);
        else if (nodo instanceof NodoSiSino) analizarSiSino((NodoSiSino) nodo);
        else if (nodo instanceof NodoMientras) analizarMientras((NodoMientras) nodo);
        else if (nodo instanceof NodoHacerMientras) analizarHacerMientras((NodoHacerMientras) nodo);
        else if (nodo instanceof NodoPara) analizarPara((NodoPara) nodo);
        else if (nodo instanceof NodoElegir) analizarElegir((NodoElegir) nodo);
        else if (nodo instanceof NodoImprimir) analizarImprimir((NodoImprimir) nodo);
        else if (nodo instanceof NodoLeer) analizarLeer((NodoLeer) nodo);
        else if (nodo instanceof NodoBloque) analizarBloque((NodoBloque) nodo);
        else if (nodo instanceof NodoRomper) { /* válido en switch/bucle */ }
    }

    private void analizarDeclaracion(NodoDeclVariable nodo) {
        if (tablaVariables.containsKey(nodo.nombre)) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: Variable '" + nodo.nombre + "' ya fue declarada");
            return;
        }
        tablaVariables.put(nodo.nombre, nodo.tipoDato);

        if (nodo.valorInicial != null) {
            String tipoExpr = inferirTipo(nodo.valorInicial);
            verificarCompatibilidad(nodo.tipoDato, tipoExpr, nodo.linea, nodo.nombre);
        }
    }

    private void analizarAsignacion(NodoAsignacion nodo) {
        if (!tablaVariables.containsKey(nodo.nombre)) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: Variable '" + nodo.nombre + "' no ha sido declarada");
            return;
        }
        String tipoVar = tablaVariables.get(nodo.nombre);
        String tipoExpr = inferirTipo(nodo.valor);
        verificarCompatibilidad(tipoVar, tipoExpr, nodo.linea, nodo.nombre);
    }

    private void analizarSiSino(NodoSiSino nodo) {
        String tipoCond = inferirTipo(nodo.condicion);
        if (!tipoCond.equals("booleano") && !tipoCond.equals("entero") && !tipoCond.equals("desconocido")) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: La condición del 'si' debe ser booleana");
        }
        analizarNodo(nodo.bloqueSi);
        if (nodo.bloqueSino != null) analizarNodo(nodo.bloqueSino);
    }

    private void analizarMientras(NodoMientras nodo) {
        String tipoCond = inferirTipo(nodo.condicion);
        if (!tipoCond.equals("booleano") && !tipoCond.equals("entero") && !tipoCond.equals("desconocido")) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: La condición del 'mientras' debe ser booleana");
        }
        analizarNodo(nodo.cuerpo);
    }

    private void analizarHacerMientras(NodoHacerMientras nodo) {
        analizarNodo(nodo.cuerpo);
        String tipoCond = inferirTipo(nodo.condicion);
        if (!tipoCond.equals("booleano") && !tipoCond.equals("entero") && !tipoCond.equals("desconocido")) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: La condición del 'hacer-mientras' debe ser booleana");
        }
    }

    private void analizarPara(NodoPara nodo) {
        analizarNodo(nodo.inicializacion);
        inferirTipo(nodo.condicion);
        analizarNodo(nodo.actualizacion);
        analizarNodo(nodo.cuerpo);
    }

    private void analizarElegir(NodoElegir nodo) {
        String tipoExpr = inferirTipo(nodo.expresion);
        for (NodoCaso caso : nodo.casos) {
            String tipoCaso = inferirTipo(caso.valor);
            if (!tipoExpr.equals(tipoCaso) && !tipoExpr.equals("desconocido") && !tipoCaso.equals("desconocido")) {
                advertencias.add("Advertencia [Línea " + caso.linea + "]: Tipo del caso (" + tipoCaso + ") difiere del tipo de la expresión (" + tipoExpr + ")");
            }
            for (NodoAST s : caso.sentencias) analizarNodo(s);
        }
        if (nodo.bloqueDefecto != null) {
            for (NodoAST s : nodo.bloqueDefecto.sentencias) analizarNodo(s);
        }
    }

    private void analizarImprimir(NodoImprimir nodo) {
        inferirTipo(nodo.expresion);
    }

    private void analizarLeer(NodoLeer nodo) {
        if (!tablaVariables.containsKey(nodo.variable)) {
            errores.add("Error Semántico [Línea " + nodo.linea + "]: Variable '" + nodo.variable + "' no ha sido declarada para leer");
        }
    }

    private void analizarBloque(NodoBloque nodo) {
        for (NodoAST s : nodo.sentencias) analizarNodo(s);
    }

    private String inferirTipo(NodoAST nodo) {
        if (nodo instanceof NodoNumeroEntero) return "entero";
        if (nodo instanceof NodoNumeroDecimal) return "decimal";
        if (nodo instanceof NodoCadenaLiteral) return "cadena";
        if (nodo instanceof NodoBooleano) return "booleano";
        if (nodo instanceof NodoIdentificador) {
            String nombre = ((NodoIdentificador) nodo).nombre;
            if (tablaVariables.containsKey(nombre)) return tablaVariables.get(nombre);
            errores.add("Error Semántico [Línea " + nodo.linea + "]: Variable '" + nombre + "' no declarada");
            return "desconocido";
        }
        if (nodo instanceof NodoExpresionBinaria) {
            NodoExpresionBinaria bin = (NodoExpresionBinaria) nodo;
            String tipoIzq = inferirTipo(bin.izquierdo);
            String tipoDer = inferirTipo(bin.derecho);
            String op = bin.operador;

            // Operadores de comparación siempre retornan booleano
            if (op.equals("==") || op.equals("!=") || op.equals("<") || op.equals(">") ||
                op.equals("<=") || op.equals(">=")) {
                return "booleano";
            }
            // Operadores lógicos
            if (op.equals("&&") || op.equals("||")) return "booleano";

            // Concatenación de cadenas
            if (op.equals("+") && (tipoIzq.equals("cadena") || tipoDer.equals("cadena"))) {
                return "cadena";
            }

            // Aritmética
            if (tipoIzq.equals("decimal") || tipoDer.equals("decimal")) return "decimal";
            if (tipoIzq.equals("entero") && tipoDer.equals("entero")) return "entero";

            // Verificar tipos incompatibles
            if (!tipoIzq.equals("desconocido") && !tipoDer.equals("desconocido")) {
                if ((tipoIzq.equals("cadena") && !tipoDer.equals("cadena") && !op.equals("+")) ||
                    (tipoDer.equals("cadena") && !tipoIzq.equals("cadena") && !op.equals("+"))) {
                    errores.add("Error Semántico [Línea " + nodo.linea + "]: Operación '" + op + "' incompatible entre " + tipoIzq + " y " + tipoDer);
                }
            }
            return "desconocido";
        }
        if (nodo instanceof NodoExpresionUnaria) {
            NodoExpresionUnaria un = (NodoExpresionUnaria) nodo;
            return inferirTipo(un.operando);
        }
        return "desconocido";
    }

    private void verificarCompatibilidad(String tipoDest, String tipoFuente, int linea, String nombre) {
        if (tipoFuente.equals("desconocido")) return;
        if (tipoDest.equals(tipoFuente)) return;
        if (tipoDest.equals("decimal") && tipoFuente.equals("entero")) return; // promoción implícita
        if (tipoDest.equals("cadena")) return; // cadena acepta todo (conversión implícita)
        errores.add("Error Semántico [Línea " + linea + "]: No se puede asignar " + tipoFuente + " a variable '" + nombre + "' de tipo " + tipoDest);
    }

    public void mostrarTablaSemantica() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║          TABLA DE SÍMBOLOS (SEMÁNTICO)           ║");
        System.out.println("╠═══════════════════════╦══════════════════════════╣");
        System.out.printf("║ %-21s ║ %-24s ║\n", "Variable", "Tipo de Dato");
        System.out.println("╠═══════════════════════╬══════════════════════════╣");
        for (Map.Entry<String, String> e : tablaVariables.entrySet()) {
            System.out.printf("║ %-21s ║ %-24s ║\n", e.getKey(), e.getValue());
        }
        System.out.println("╚═══════════════════════╩══════════════════════════╝");
    }

    public Map<String, String> getTablaVariables() { return tablaVariables; }
    public List<String> getErrores() { return errores; }
    public List<String> getAdvertencias() { return advertencias; }
}
