public class Token {
    public enum Tipo {
        // Palabras reservadas
        ENTERO, DECIMAL, CADENA, BOOLEANO,
        SI, SINO, MIENTRAS, HACER, PARA, ELEGIR, CASO, DEFECTO,
        IMPRIMIR, LEER, VERDADERO, FALSO, ROMPER,
        // Literales e identificadores
        ID, NUM_ENTERO, NUM_DECIMAL, CADENA_LITERAL,
        // Operadores aritméticos
        SUMA, RESTA, MULTIPLICACION, DIVISION,
        // Operadores de comparación
        IGUAL_IGUAL, DIFERENTE, MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL,
        // Operadores lógicos
        Y, O, NO,
        // Asignación
        IGUAL,
        // Delimitadores
        PARENTESIS_IZQ, PARENTESIS_DER,
        LLAVE_IZQ, LLAVE_DER,
        PUNTO_COMA, DOS_PUNTOS, COMA,
        // Fin de archivo
        EOF
    }

    public Tipo tipo;
    public String lexema;
    public int linea;
    public int columna;

    public Token(Tipo tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public String toString() {
        return String.format("<%-18s, \"%s\"> [Línea %d, Col %d]", tipo, lexema, linea, columna);
    }
}
