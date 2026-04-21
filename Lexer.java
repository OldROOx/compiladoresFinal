import java.util.*;

public class Lexer {
    private String fuente;
    private int pos;
    private int linea;
    private int columna;
    private char charActual;
    private List<String> errores = new ArrayList<>();
    private Map<String, Token.Tipo> palabrasReservadas = new HashMap<>();
    private Map<String, String> tablaSimbolos = new LinkedHashMap<>();

    public Lexer(String fuente) {
        this.fuente = fuente;
        this.pos = 0;
        this.linea = 1;
        this.columna = 1;
        this.charActual = fuente.length() > 0 ? fuente.charAt(0) : '\0';
        inicializarPalabrasReservadas();
    }

    private void inicializarPalabrasReservadas() {
        palabrasReservadas.put("entero", Token.Tipo.ENTERO);
        palabrasReservadas.put("decimal", Token.Tipo.DECIMAL);
        palabrasReservadas.put("cadena", Token.Tipo.CADENA);
        palabrasReservadas.put("booleano", Token.Tipo.BOOLEANO);
        palabrasReservadas.put("si", Token.Tipo.SI);
        palabrasReservadas.put("sino", Token.Tipo.SINO);
        palabrasReservadas.put("mientras", Token.Tipo.MIENTRAS);
        palabrasReservadas.put("hacer", Token.Tipo.HACER);
        palabrasReservadas.put("para", Token.Tipo.PARA);
        palabrasReservadas.put("elegir", Token.Tipo.ELEGIR);
        palabrasReservadas.put("caso", Token.Tipo.CASO);
        palabrasReservadas.put("defecto", Token.Tipo.DEFECTO);
        palabrasReservadas.put("imprimir", Token.Tipo.IMPRIMIR);
        palabrasReservadas.put("leer", Token.Tipo.LEER);
        palabrasReservadas.put("verdadero", Token.Tipo.VERDADERO);
        palabrasReservadas.put("falso", Token.Tipo.FALSO);
        palabrasReservadas.put("romper", Token.Tipo.ROMPER);
    }

    private void avanzar() {
        pos++;
        columna++;
        if (pos < fuente.length()) {
            charActual = fuente.charAt(pos);
            if (charActual == '\n') {
                linea++;
                columna = 0;
            }
        } else {
            charActual = '\0';
        }
    }

    private char verSiguiente() {
        int siguiente = pos + 1;
        return siguiente < fuente.length() ? fuente.charAt(siguiente) : '\0';
    }

    private void saltarEspacios() {
        while (charActual != '\0' && Character.isWhitespace(charActual)) {
            avanzar();
        }
    }

    private void saltarComentario() {
        if (charActual == '/' && verSiguiente() == '/') {
            while (charActual != '\0' && charActual != '\n') {
                avanzar();
            }
        }
    }

    private Token leerNumero() {
        StringBuilder sb = new StringBuilder();
        int lineaInicio = linea;
        int colInicio = columna;
        boolean esDecimal = false;

        while (charActual != '\0' && Character.isDigit(charActual)) {
            sb.append(charActual);
            avanzar();
        }

        if (charActual == '.' && Character.isDigit(verSiguiente())) {
            esDecimal = true;
            sb.append(charActual);
            avanzar();
            while (charActual != '\0' && Character.isDigit(charActual)) {
                sb.append(charActual);
                avanzar();
            }
        }

        String lexema = sb.toString();
        Token.Tipo tipo = esDecimal ? Token.Tipo.NUM_DECIMAL : Token.Tipo.NUM_ENTERO;
        tablaSimbolos.put(lexema, tipo.toString());
        return new Token(tipo, lexema, lineaInicio, colInicio);
    }

    private Token leerIdentificador() {
        StringBuilder sb = new StringBuilder();
        int lineaInicio = linea;
        int colInicio = columna;

        while (charActual != '\0' && (Character.isLetterOrDigit(charActual) || charActual == '_')) {
            sb.append(charActual);
            avanzar();
        }

        String lexema = sb.toString();
        Token.Tipo tipo = palabrasReservadas.getOrDefault(lexema, Token.Tipo.ID);
        tablaSimbolos.put(lexema, tipo.toString());
        return new Token(tipo, lexema, lineaInicio, colInicio);
    }

    private Token leerCadena() {
        StringBuilder sb = new StringBuilder();
        int lineaInicio = linea;
        int colInicio = columna;
        avanzar(); // saltar comilla inicial

        while (charActual != '\0' && charActual != '"') {
            if (charActual == '\\') {
                avanzar();
                switch (charActual) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(charActual);
                }
            } else {
                sb.append(charActual);
            }
            avanzar();
        }

        if (charActual == '\0') {
            errores.add("Error Léxico [Línea " + lineaInicio + ", Col " + colInicio + "]: Cadena sin cerrar");
            return new Token(Token.Tipo.CADENA_LITERAL, sb.toString(), lineaInicio, colInicio);
        }

        avanzar(); // saltar comilla final
        return new Token(Token.Tipo.CADENA_LITERAL, sb.toString(), lineaInicio, colInicio);
    }

    public List<Token> escanear() {
        List<Token> tokens = new ArrayList<>();

        while (charActual != '\0') {
            saltarEspacios();
            if (charActual == '\0') break;

            // Comentarios
            if (charActual == '/' && verSiguiente() == '/') {
                saltarComentario();
                continue;
            }

            int lineaActual = linea;
            int colActual = columna;

            // Identificadores y palabras reservadas
            if (Character.isLetter(charActual) || charActual == '_') {
                tokens.add(leerIdentificador());
                continue;
            }

            // Números
            if (Character.isDigit(charActual)) {
                tokens.add(leerNumero());
                continue;
            }

            // Cadenas
            if (charActual == '"') {
                tokens.add(leerCadena());
                continue;
            }

            // Operadores y delimitadores
            switch (charActual) {
                case '+': tokens.add(new Token(Token.Tipo.SUMA, "+", lineaActual, colActual)); avanzar(); break;
                case '-': tokens.add(new Token(Token.Tipo.RESTA, "-", lineaActual, colActual)); avanzar(); break;
                case '*': tokens.add(new Token(Token.Tipo.MULTIPLICACION, "*", lineaActual, colActual)); avanzar(); break;
                case '/': tokens.add(new Token(Token.Tipo.DIVISION, "/", lineaActual, colActual)); avanzar(); break;
                case '(':  tokens.add(new Token(Token.Tipo.PARENTESIS_IZQ, "(", lineaActual, colActual)); avanzar(); break;
                case ')': tokens.add(new Token(Token.Tipo.PARENTESIS_DER, ")", lineaActual, colActual)); avanzar(); break;
                case '{': tokens.add(new Token(Token.Tipo.LLAVE_IZQ, "{", lineaActual, colActual)); avanzar(); break;
                case '}': tokens.add(new Token(Token.Tipo.LLAVE_DER, "}", lineaActual, colActual)); avanzar(); break;
                case ';': tokens.add(new Token(Token.Tipo.PUNTO_COMA, ";", lineaActual, colActual)); avanzar(); break;
                case ':': tokens.add(new Token(Token.Tipo.DOS_PUNTOS, ":", lineaActual, colActual)); avanzar(); break;
                case ',': tokens.add(new Token(Token.Tipo.COMA, ",", lineaActual, colActual)); avanzar(); break;
                case '=':
                    avanzar();
                    if (charActual == '=') {
                        tokens.add(new Token(Token.Tipo.IGUAL_IGUAL, "==", lineaActual, colActual));
                        avanzar();
                    } else {
                        tokens.add(new Token(Token.Tipo.IGUAL, "=", lineaActual, colActual));
                    }
                    break;
                case '!':
                    avanzar();
                    if (charActual == '=') {
                        tokens.add(new Token(Token.Tipo.DIFERENTE, "!=", lineaActual, colActual));
                        avanzar();
                    } else {
                        tokens.add(new Token(Token.Tipo.NO, "!", lineaActual, colActual));
                    }
                    break;
                case '<':
                    avanzar();
                    if (charActual == '=') {
                        tokens.add(new Token(Token.Tipo.MENOR_IGUAL, "<=", lineaActual, colActual));
                        avanzar();
                    } else {
                        tokens.add(new Token(Token.Tipo.MENOR, "<", lineaActual, colActual));
                    }
                    break;
                case '>':
                    avanzar();
                    if (charActual == '=') {
                        tokens.add(new Token(Token.Tipo.MAYOR_IGUAL, ">=", lineaActual, colActual));
                        avanzar();
                    } else {
                        tokens.add(new Token(Token.Tipo.MAYOR, ">", lineaActual, colActual));
                    }
                    break;
                case '&':
                    avanzar();
                    if (charActual == '&') {
                        tokens.add(new Token(Token.Tipo.Y, "&&", lineaActual, colActual));
                        avanzar();
                    } else {
                        errores.add("Error Léxico [Línea " + lineaActual + ", Col " + colActual + "]: Se esperaba '&&'");
                    }
                    break;
                case '|':
                    avanzar();
                    if (charActual == '|') {
                        tokens.add(new Token(Token.Tipo.O, "||", lineaActual, colActual));
                        avanzar();
                    } else {
                        errores.add("Error Léxico [Línea " + lineaActual + ", Col " + colActual + "]: Se esperaba '||'");
                    }
                    break;
                default:
                    errores.add("Error Léxico [Línea " + lineaActual + ", Col " + colActual + "]: Carácter no reconocido '" + charActual + "'");
                    avanzar();
            }
        }

        tokens.add(new Token(Token.Tipo.EOF, "EOF", linea, columna));
        return tokens;
    }

    public List<String> getErrores() { return errores; }
    public Map<String, String> getTablaSimbolos() { return tablaSimbolos; }

    public void mostrarTablaSimbolos() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║            TABLA DE SÍMBOLOS (LÉXICO)            ║");
        System.out.println("╠═══════════════════════╦══════════════════════════╣");
        System.out.printf("║ %-21s ║ %-24s ║\n", "Lexema", "Categoría");
        System.out.println("╠═══════════════════════╬══════════════════════════╣");
        for (Map.Entry<String, String> e : tablaSimbolos.entrySet()) {
            String lex = e.getKey().length() > 21 ? e.getKey().substring(0, 18) + "..." : e.getKey();
            System.out.printf("║ %-21s ║ %-24s ║\n", lex, e.getValue());
        }
        System.out.println("╚═══════════════════════╩══════════════════════════╝");
    }
}
