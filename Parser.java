import java.util.*;

public class Parser {
    private List<Token> tokens;
    private int pos;
    private Token tokenActual;
    private List<String> errores = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.tokenActual = tokens.get(0);
    }

    private Token consumir(Token.Tipo tipo) {
        if (tokenActual.tipo == tipo) {
            Token t = tokenActual;
            avanzar();
            return t;
        }
        error("Se esperaba " + tipo + " pero se encontró " + tokenActual.tipo + " ('" + tokenActual.lexema + "')");
        return tokenActual;
    }

    private void avanzar() {
        pos++;
        if (pos < tokens.size()) {
            tokenActual = tokens.get(pos);
        }
    }

    private void error(String msg) {
        String err = "Error Sintáctico [Línea " + tokenActual.linea + ", Col " + tokenActual.columna + "]: " + msg;
        errores.add(err);
        // Recuperación: intentar sincronizar con punto y coma o llave
        while (tokenActual.tipo != Token.Tipo.PUNTO_COMA &&
               tokenActual.tipo != Token.Tipo.LLAVE_DER &&
               tokenActual.tipo != Token.Tipo.EOF) {
            avanzar();
        }
        if (tokenActual.tipo == Token.Tipo.PUNTO_COMA) avanzar();
    }

    public NodoPrograma parsear() {
        NodoPrograma programa = new NodoPrograma();
        while (tokenActual.tipo != Token.Tipo.EOF) {
            try {
                NodoAST sentencia = parsearSentencia();
                if (sentencia != null) {
                    programa.sentencias.add(sentencia);
                }
            } catch (Exception e) {
                error(e.getMessage() != null ? e.getMessage() : "Error inesperado");
                if (tokenActual.tipo == Token.Tipo.EOF) break;
            }
        }
        return programa;
    }

    private NodoAST parsearSentencia() {
        switch (tokenActual.tipo) {
            case ENTERO: case DECIMAL: case CADENA: case BOOLEANO:
                return parsearDeclaracion();
            case SI:
                return parsearSiSino();
            case MIENTRAS:
                return parsearMientras();
            case HACER:
                return parsearHacerMientras();
            case PARA:
                return parsearPara();
            case ELEGIR:
                return parsearElegir();
            case IMPRIMIR:
                return parsearImprimir();
            case LEER:
                return parsearLeer();
            case ROMPER:
                return parsearRomper();
            case ID:
                return parsearAsignacion();
            case LLAVE_IZQ:
                return parsearBloque();
            default:
                error("Sentencia inesperada: '" + tokenActual.lexema + "'");
                return null;
        }
    }

    private NodoDeclVariable parsearDeclaracion() {
        NodoDeclVariable nodo = new NodoDeclVariable();
        nodo.linea = tokenActual.linea;
        nodo.tipoDato = tokenActual.lexema;
        avanzar(); // consumir tipo

        Token id = consumir(Token.Tipo.ID);
        nodo.nombre = id.lexema;

        if (tokenActual.tipo == Token.Tipo.IGUAL) {
            avanzar();
            nodo.valorInicial = parsearExpresion();
        }

        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoAsignacion parsearAsignacion() {
        NodoAsignacion nodo = new NodoAsignacion();
        nodo.linea = tokenActual.linea;
        Token id = consumir(Token.Tipo.ID);
        nodo.nombre = id.lexema;
        consumir(Token.Tipo.IGUAL);
        nodo.valor = parsearExpresion();
        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoSiSino parsearSiSino() {
        NodoSiSino nodo = new NodoSiSino();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.SI);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        nodo.condicion = parsearExpresion();
        consumir(Token.Tipo.PARENTESIS_DER);
        nodo.bloqueSi = parsearBloque();

        if (tokenActual.tipo == Token.Tipo.SINO) {
            avanzar();
            nodo.bloqueSino = parsearBloque();
        }

        return nodo;
    }

    private NodoMientras parsearMientras() {
        NodoMientras nodo = new NodoMientras();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.MIENTRAS);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        nodo.condicion = parsearExpresion();
        consumir(Token.Tipo.PARENTESIS_DER);
        nodo.cuerpo = parsearBloque();
        return nodo;
    }

    private NodoHacerMientras parsearHacerMientras() {
        NodoHacerMientras nodo = new NodoHacerMientras();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.HACER);
        nodo.cuerpo = parsearBloque();
        consumir(Token.Tipo.MIENTRAS);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        nodo.condicion = parsearExpresion();
        consumir(Token.Tipo.PARENTESIS_DER);
        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoPara parsearPara() {
        NodoPara nodo = new NodoPara();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.PARA);
        consumir(Token.Tipo.PARENTESIS_IZQ);

        // Inicialización: puede ser declaración o asignación
        if (tokenActual.tipo == Token.Tipo.ENTERO || tokenActual.tipo == Token.Tipo.DECIMAL) {
            nodo.inicializacion = parsearDeclaracion();
        } else {
            nodo.inicializacion = parsearAsignacion();
        }

        // Condición
        nodo.condicion = parsearExpresion();
        consumir(Token.Tipo.PUNTO_COMA);

        // Actualización (sin punto y coma)
        NodoAsignacion actualizacion = new NodoAsignacion();
        actualizacion.linea = tokenActual.linea;
        Token id = consumir(Token.Tipo.ID);
        actualizacion.nombre = id.lexema;
        consumir(Token.Tipo.IGUAL);
        actualizacion.valor = parsearExpresion();
        nodo.actualizacion = actualizacion;

        consumir(Token.Tipo.PARENTESIS_DER);
        nodo.cuerpo = parsearBloque();
        return nodo;
    }

    private NodoElegir parsearElegir() {
        NodoElegir nodo = new NodoElegir();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.ELEGIR);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        nodo.expresion = parsearExpresion();
        consumir(Token.Tipo.PARENTESIS_DER);
        consumir(Token.Tipo.LLAVE_IZQ);

        while (tokenActual.tipo == Token.Tipo.CASO) {
            NodoCaso caso = new NodoCaso();
            caso.linea = tokenActual.linea;
            consumir(Token.Tipo.CASO);
            caso.valor = parsearPrimario();
            consumir(Token.Tipo.DOS_PUNTOS);

            while (tokenActual.tipo != Token.Tipo.CASO &&
                   tokenActual.tipo != Token.Tipo.DEFECTO &&
                   tokenActual.tipo != Token.Tipo.LLAVE_DER &&
                   tokenActual.tipo != Token.Tipo.EOF) {
                NodoAST s = parsearSentencia();
                if (s != null) caso.sentencias.add(s);
            }
            nodo.casos.add(caso);
        }

        if (tokenActual.tipo == Token.Tipo.DEFECTO) {
            avanzar();
            consumir(Token.Tipo.DOS_PUNTOS);
            NodoBloque bloqueDefecto = new NodoBloque();
            while (tokenActual.tipo != Token.Tipo.LLAVE_DER && tokenActual.tipo != Token.Tipo.EOF) {
                NodoAST s = parsearSentencia();
                if (s != null) bloqueDefecto.sentencias.add(s);
            }
            nodo.bloqueDefecto = bloqueDefecto;
        }

        consumir(Token.Tipo.LLAVE_DER);
        return nodo;
    }

    private NodoImprimir parsearImprimir() {
        NodoImprimir nodo = new NodoImprimir();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.IMPRIMIR);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        nodo.expresion = parsearExpresion();
        consumir(Token.Tipo.PARENTESIS_DER);
        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoLeer parsearLeer() {
        NodoLeer nodo = new NodoLeer();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.LEER);
        consumir(Token.Tipo.PARENTESIS_IZQ);
        Token id = consumir(Token.Tipo.ID);
        nodo.variable = id.lexema;
        consumir(Token.Tipo.PARENTESIS_DER);
        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoRomper parsearRomper() {
        NodoRomper nodo = new NodoRomper();
        nodo.linea = tokenActual.linea;
        consumir(Token.Tipo.ROMPER);
        consumir(Token.Tipo.PUNTO_COMA);
        return nodo;
    }

    private NodoBloque parsearBloque() {
        NodoBloque bloque = new NodoBloque();
        bloque.linea = tokenActual.linea;
        consumir(Token.Tipo.LLAVE_IZQ);
        while (tokenActual.tipo != Token.Tipo.LLAVE_DER && tokenActual.tipo != Token.Tipo.EOF) {
            NodoAST s = parsearSentencia();
            if (s != null) bloque.sentencias.add(s);
        }
        consumir(Token.Tipo.LLAVE_DER);
        return bloque;
    }

    // ── Expresiones con precedencia ──

    private NodoAST parsearExpresion() {
        return parsearO();
    }

    private NodoAST parsearO() {
        NodoAST izq = parsearY();
        while (tokenActual.tipo == Token.Tipo.O) {
            String op = tokenActual.lexema;
            avanzar();
            NodoAST der = parsearY();
            NodoExpresionBinaria nodo = new NodoExpresionBinaria();
            nodo.operador = op; nodo.izquierdo = izq; nodo.derecho = der;
            nodo.linea = izq.linea;
            izq = nodo;
        }
        return izq;
    }

    private NodoAST parsearY() {
        NodoAST izq = parsearComparacion();
        while (tokenActual.tipo == Token.Tipo.Y) {
            String op = tokenActual.lexema;
            avanzar();
            NodoAST der = parsearComparacion();
            NodoExpresionBinaria nodo = new NodoExpresionBinaria();
            nodo.operador = op; nodo.izquierdo = izq; nodo.derecho = der;
            nodo.linea = izq.linea;
            izq = nodo;
        }
        return izq;
    }

    private NodoAST parsearComparacion() {
        NodoAST izq = parsearAditiva();
        if (tokenActual.tipo == Token.Tipo.IGUAL_IGUAL ||
            tokenActual.tipo == Token.Tipo.DIFERENTE ||
            tokenActual.tipo == Token.Tipo.MENOR ||
            tokenActual.tipo == Token.Tipo.MAYOR ||
            tokenActual.tipo == Token.Tipo.MENOR_IGUAL ||
            tokenActual.tipo == Token.Tipo.MAYOR_IGUAL) {
            String op = tokenActual.lexema;
            avanzar();
            NodoAST der = parsearAditiva();
            NodoExpresionBinaria nodo = new NodoExpresionBinaria();
            nodo.operador = op; nodo.izquierdo = izq; nodo.derecho = der;
            nodo.linea = izq.linea;
            return nodo;
        }
        return izq;
    }

    private NodoAST parsearAditiva() {
        NodoAST izq = parsearMultiplicativa();
        while (tokenActual.tipo == Token.Tipo.SUMA || tokenActual.tipo == Token.Tipo.RESTA) {
            String op = tokenActual.lexema;
            avanzar();
            NodoAST der = parsearMultiplicativa();
            NodoExpresionBinaria nodo = new NodoExpresionBinaria();
            nodo.operador = op; nodo.izquierdo = izq; nodo.derecho = der;
            nodo.linea = izq.linea;
            izq = nodo;
        }
        return izq;
    }

    private NodoAST parsearMultiplicativa() {
        NodoAST izq = parsearUnario();
        while (tokenActual.tipo == Token.Tipo.MULTIPLICACION || tokenActual.tipo == Token.Tipo.DIVISION) {
            String op = tokenActual.lexema;
            avanzar();
            NodoAST der = parsearUnario();
            NodoExpresionBinaria nodo = new NodoExpresionBinaria();
            nodo.operador = op; nodo.izquierdo = izq; nodo.derecho = der;
            nodo.linea = izq.linea;
            izq = nodo;
        }
        return izq;
    }

    private NodoAST parsearUnario() {
        if (tokenActual.tipo == Token.Tipo.RESTA) {
            avanzar();
            NodoExpresionUnaria nodo = new NodoExpresionUnaria();
            nodo.operador = "-";
            nodo.operando = parsearPrimario();
            nodo.linea = nodo.operando.linea;
            return nodo;
        }
        if (tokenActual.tipo == Token.Tipo.NO) {
            avanzar();
            NodoExpresionUnaria nodo = new NodoExpresionUnaria();
            nodo.operador = "!";
            nodo.operando = parsearPrimario();
            nodo.linea = nodo.operando.linea;
            return nodo;
        }
        return parsearPrimario();
    }

    private NodoAST parsearPrimario() {
        switch (tokenActual.tipo) {
            case NUM_ENTERO: {
                NodoNumeroEntero nodo = new NodoNumeroEntero();
                nodo.valor = Integer.parseInt(tokenActual.lexema);
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case NUM_DECIMAL: {
                NodoNumeroDecimal nodo = new NodoNumeroDecimal();
                nodo.valor = Double.parseDouble(tokenActual.lexema);
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case CADENA_LITERAL: {
                NodoCadenaLiteral nodo = new NodoCadenaLiteral();
                nodo.valor = tokenActual.lexema;
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case VERDADERO: {
                NodoBooleano nodo = new NodoBooleano();
                nodo.valor = true;
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case FALSO: {
                NodoBooleano nodo = new NodoBooleano();
                nodo.valor = false;
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case ID: {
                NodoIdentificador nodo = new NodoIdentificador();
                nodo.nombre = tokenActual.lexema;
                nodo.linea = tokenActual.linea;
                avanzar();
                return nodo;
            }
            case PARENTESIS_IZQ: {
                avanzar();
                NodoAST expr = parsearExpresion();
                consumir(Token.Tipo.PARENTESIS_DER);
                return expr;
            }
            default:
                error("Expresión inesperada: '" + tokenActual.lexema + "'");
                NodoNumeroEntero fallback = new NodoNumeroEntero();
                fallback.valor = 0;
                fallback.linea = tokenActual.linea;
                return fallback;
        }
    }

    public List<String> getErrores() { return errores; }
}
