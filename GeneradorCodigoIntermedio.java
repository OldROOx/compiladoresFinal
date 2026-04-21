import java.util.*;

public class GeneradorCodigoIntermedio {
    private List<Cuadrupla> cuadruplas = new ArrayList<>();
    private int tempCount = 0;
    private int etiquetaCount = 0;
    private List<String> errores = new ArrayList<>();

    public static class Cuadrupla {
        public int indice;
        public String operador;
        public String arg1;
        public String arg2;
        public String resultado;

        public Cuadrupla(int indice, String operador, String arg1, String arg2, String resultado) {
            this.indice = indice;
            this.operador = operador;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.resultado = resultado;
        }

        @Override
        public String toString() {
            return String.format("%-6d %-12s %-12s %-12s %-12s",
                indice,
                operador != null ? operador : "-",
                arg1 != null ? arg1 : "-",
                arg2 != null ? arg2 : "-",
                resultado != null ? resultado : "-");
        }
    }

    private String nuevoTemp() { return "t" + (tempCount++); }
    private String nuevaEtiqueta() { return "L" + (etiquetaCount++); }

    public void generar(NodoPrograma programa) {
        for (NodoAST sentencia : programa.sentencias) {
            generarNodo(sentencia);
        }
    }

    private String generarNodo(NodoAST nodo) {
        if (nodo instanceof NodoDeclVariable) return generarDeclaracion((NodoDeclVariable) nodo);
        if (nodo instanceof NodoAsignacion) return generarAsignacion((NodoAsignacion) nodo);
        if (nodo instanceof NodoSiSino) { generarSiSino((NodoSiSino) nodo); return null; }
        if (nodo instanceof NodoMientras) { generarMientras((NodoMientras) nodo); return null; }
        if (nodo instanceof NodoHacerMientras) { generarHacerMientras((NodoHacerMientras) nodo); return null; }
        if (nodo instanceof NodoPara) { generarPara((NodoPara) nodo); return null; }
        if (nodo instanceof NodoElegir) { generarElegir((NodoElegir) nodo); return null; }
        if (nodo instanceof NodoImprimir) { generarImprimir((NodoImprimir) nodo); return null; }
        if (nodo instanceof NodoLeer) { generarLeer((NodoLeer) nodo); return null; }
        if (nodo instanceof NodoBloque) { generarBloque((NodoBloque) nodo); return null; }
        if (nodo instanceof NodoRomper) { /* se maneja en switch */ return null; }
        if (nodo instanceof NodoExpresionBinaria) return generarExpresionBinaria((NodoExpresionBinaria) nodo);
        if (nodo instanceof NodoExpresionUnaria) return generarExpresionUnaria((NodoExpresionUnaria) nodo);
        if (nodo instanceof NodoNumeroEntero) return String.valueOf(((NodoNumeroEntero) nodo).valor);
        if (nodo instanceof NodoNumeroDecimal) return String.valueOf(((NodoNumeroDecimal) nodo).valor);
        if (nodo instanceof NodoCadenaLiteral) return "\"" + ((NodoCadenaLiteral) nodo).valor + "\"";
        if (nodo instanceof NodoBooleano) return ((NodoBooleano) nodo).valor ? "1" : "0";
        if (nodo instanceof NodoIdentificador) return ((NodoIdentificador) nodo).nombre;
        return null;
    }

    private void emitir(String op, String arg1, String arg2, String resultado) {
        cuadruplas.add(new Cuadrupla(cuadruplas.size(), op, arg1, arg2, resultado));
    }

    private String generarDeclaracion(NodoDeclVariable nodo) {
        emitir("DECL", nodo.tipoDato, null, nodo.nombre);
        if (nodo.valorInicial != null) {
            String val = generarNodo(nodo.valorInicial);
            emitir("=", val, null, nodo.nombre);
        }
        return nodo.nombre;
    }

    private String generarAsignacion(NodoAsignacion nodo) {
        String val = generarNodo(nodo.valor);
        emitir("=", val, null, nodo.nombre);
        return nodo.nombre;
    }

    private String generarExpresionBinaria(NodoExpresionBinaria nodo) {
        String izq = generarNodo(nodo.izquierdo);
        String der = generarNodo(nodo.derecho);
        String temp = nuevoTemp();
        emitir(nodo.operador, izq, der, temp);
        return temp;
    }

    private String generarExpresionUnaria(NodoExpresionUnaria nodo) {
        String operando = generarNodo(nodo.operando);
        String temp = nuevoTemp();
        emitir(nodo.operador, operando, null, temp);
        return temp;
    }

    private void generarSiSino(NodoSiSino nodo) {
        String cond = generarNodo(nodo.condicion);
        String etiquetaFalso = nuevaEtiqueta();
        String etiquetaFin = nuevaEtiqueta();

        emitir("IF_FALSE", cond, null, etiquetaFalso);
        generarNodo(nodo.bloqueSi);

        if (nodo.bloqueSino != null) {
            emitir("GOTO", null, null, etiquetaFin);
            emitir("LABEL", null, null, etiquetaFalso);
            generarNodo(nodo.bloqueSino);
            emitir("LABEL", null, null, etiquetaFin);
        } else {
            emitir("LABEL", null, null, etiquetaFalso);
        }
    }

    private void generarMientras(NodoMientras nodo) {
        String etiquetaInicio = nuevaEtiqueta();
        String etiquetaFin = nuevaEtiqueta();

        emitir("LABEL", null, null, etiquetaInicio);
        String cond = generarNodo(nodo.condicion);
        emitir("IF_FALSE", cond, null, etiquetaFin);
        generarNodo(nodo.cuerpo);
        emitir("GOTO", null, null, etiquetaInicio);
        emitir("LABEL", null, null, etiquetaFin);
    }

    private void generarHacerMientras(NodoHacerMientras nodo) {
        String etiquetaInicio = nuevaEtiqueta();

        emitir("LABEL", null, null, etiquetaInicio);
        generarNodo(nodo.cuerpo);
        String cond = generarNodo(nodo.condicion);
        emitir("IF_TRUE", cond, null, etiquetaInicio);
    }

    private void generarPara(NodoPara nodo) {
        String etiquetaInicio = nuevaEtiqueta();
        String etiquetaFin = nuevaEtiqueta();

        generarNodo(nodo.inicializacion);
        emitir("LABEL", null, null, etiquetaInicio);
        String cond = generarNodo(nodo.condicion);
        emitir("IF_FALSE", cond, null, etiquetaFin);
        generarNodo(nodo.cuerpo);
        generarNodo(nodo.actualizacion);
        emitir("GOTO", null, null, etiquetaInicio);
        emitir("LABEL", null, null, etiquetaFin);
    }

    private void generarElegir(NodoElegir nodo) {
        String expr = generarNodo(nodo.expresion);
        String etiquetaFin = nuevaEtiqueta();
        List<String> etiquetasCasos = new ArrayList<>();

        // Generar comparaciones
        for (NodoCaso caso : nodo.casos) {
            String etiquetaCaso = nuevaEtiqueta();
            etiquetasCasos.add(etiquetaCaso);
            String valCaso = generarNodo(caso.valor);
            String temp = nuevoTemp();
            emitir("==", expr, valCaso, temp);
            emitir("IF_TRUE", temp, null, etiquetaCaso);
        }

        // Ir a defecto o fin
        String etiquetaDefecto = nuevaEtiqueta();
        if (nodo.bloqueDefecto != null) {
            emitir("GOTO", null, null, etiquetaDefecto);
        } else {
            emitir("GOTO", null, null, etiquetaFin);
        }

        // Generar cuerpo de cada caso
        for (int i = 0; i < nodo.casos.size(); i++) {
            emitir("LABEL", null, null, etiquetasCasos.get(i));
            for (NodoAST s : nodo.casos.get(i).sentencias) {
                if (s instanceof NodoRomper) {
                    emitir("GOTO", null, null, etiquetaFin);
                } else {
                    generarNodo(s);
                }
            }
        }

        if (nodo.bloqueDefecto != null) {
            emitir("LABEL", null, null, etiquetaDefecto);
            for (NodoAST s : nodo.bloqueDefecto.sentencias) {
                if (s instanceof NodoRomper) {
                    emitir("GOTO", null, null, etiquetaFin);
                } else {
                    generarNodo(s);
                }
            }
        }

        emitir("LABEL", null, null, etiquetaFin);
    }

    private void generarImprimir(NodoImprimir nodo) {
        String val = generarNodo(nodo.expresion);
        emitir("PRINT", val, null, null);
    }

    private void generarLeer(NodoLeer nodo) {
        emitir("READ", null, null, nodo.variable);
    }

    private void generarBloque(NodoBloque nodo) {
        for (NodoAST s : nodo.sentencias) generarNodo(s);
    }

    public void mostrarCuadruplas() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  CÓDIGO INTERMEDIO (CUÁDRUPLAS)                   ║");
        System.out.println("╠══════╦════════════╦════════════╦════════════╦════════════════════╣");
        System.out.printf("║ %-4s ║ %-10s ║ %-10s ║ %-10s ║ %-18s ║\n", "#", "Operador", "Arg1", "Arg2", "Resultado");
        System.out.println("╠══════╬════════════╬════════════╬════════════╬════════════════════╣");
        for (Cuadrupla c : cuadruplas) {
            String op = c.operador != null ? c.operador : "-";
            String a1 = c.arg1 != null ? c.arg1 : "-";
            String a2 = c.arg2 != null ? c.arg2 : "-";
            String res = c.resultado != null ? c.resultado : "-";
            if (a1.length() > 10) a1 = a1.substring(0, 10);
            if (a2.length() > 10) a2 = a2.substring(0, 10);
            if (res.length() > 18) res = res.substring(0, 18);
            System.out.printf("║ %-4d ║ %-10s ║ %-10s ║ %-10s ║ %-18s ║\n", c.indice, op, a1, a2, res);
        }
        System.out.println("╚══════╩════════════╩════════════╩════════════╩════════════════════╝");
    }

    public List<Cuadrupla> getCuadruplas() { return cuadruplas; }
    public List<String> getErrores() { return errores; }
}
