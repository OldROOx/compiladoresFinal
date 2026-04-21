import java.util.List;
import java.util.ArrayList;

// Clase base para todos los nodos del AST
abstract class NodoAST {
    public int linea;
    public abstract String tipoNodo();
    public abstract void imprimir(String prefijo, boolean esUltimo);
}

// ── Programa ──
class NodoPrograma extends NodoAST {
    List<NodoAST> sentencias = new ArrayList<>();
    public String tipoNodo() { return "Programa"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        System.out.println(prefijo + "╗ PROGRAMA");
        for (int i = 0; i < sentencias.size(); i++) {
            sentencias.get(i).imprimir(prefijo + "  ", i == sentencias.size() - 1);
        }
    }
}

// ── Bloque ──
class NodoBloque extends NodoAST {
    List<NodoAST> sentencias = new ArrayList<>();
    public String tipoNodo() { return "Bloque"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "BLOQUE");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        for (int i = 0; i < sentencias.size(); i++) {
            sentencias.get(i).imprimir(nuevoPrefijo, i == sentencias.size() - 1);
        }
    }
}

// ── Declaración de variable ──
class NodoDeclVariable extends NodoAST {
    String tipoDato;
    String nombre;
    NodoAST valorInicial; // puede ser null

    public String tipoNodo() { return "DeclVariable"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "DECLARAR " + tipoDato + " " + nombre);
        if (valorInicial != null) {
            String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
            valorInicial.imprimir(nuevoPrefijo, true);
        }
    }
}

// ── Asignación ──
class NodoAsignacion extends NodoAST {
    String nombre;
    NodoAST valor;

    public String tipoNodo() { return "Asignacion"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "ASIGNAR → " + nombre);
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        valor.imprimir(nuevoPrefijo, true);
    }
}

// ── Si-Sino (if-else) ──
class NodoSiSino extends NodoAST {
    NodoAST condicion;
    NodoAST bloqueSi;
    NodoAST bloqueSino; // puede ser null

    public String tipoNodo() { return "SiSino"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "SI");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        condicion.imprimir(nuevoPrefijo, false);
        bloqueSi.imprimir(nuevoPrefijo, bloqueSino == null);
        if (bloqueSino != null) {
            System.out.println(nuevoPrefijo + "├── SINO");
            bloqueSino.imprimir(nuevoPrefijo, true);
        }
    }
}

// ── Mientras (while) ──
class NodoMientras extends NodoAST {
    NodoAST condicion;
    NodoAST cuerpo;

    public String tipoNodo() { return "Mientras"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "MIENTRAS");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        condicion.imprimir(nuevoPrefijo, false);
        cuerpo.imprimir(nuevoPrefijo, true);
    }
}

// ── Hacer-Mientras (do-while) ──
class NodoHacerMientras extends NodoAST {
    NodoAST cuerpo;
    NodoAST condicion;

    public String tipoNodo() { return "HacerMientras"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "HACER-MIENTRAS");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        cuerpo.imprimir(nuevoPrefijo, false);
        condicion.imprimir(nuevoPrefijo, true);
    }
}

// ── Para (for) ──
class NodoPara extends NodoAST {
    NodoAST inicializacion;
    NodoAST condicion;
    NodoAST actualizacion;
    NodoAST cuerpo;

    public String tipoNodo() { return "Para"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "PARA");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        inicializacion.imprimir(nuevoPrefijo, false);
        condicion.imprimir(nuevoPrefijo, false);
        actualizacion.imprimir(nuevoPrefijo, false);
        cuerpo.imprimir(nuevoPrefijo, true);
    }
}

// ── Elegir (switch-case) ──
class NodoElegir extends NodoAST {
    NodoAST expresion;
    List<NodoCaso> casos = new ArrayList<>();
    NodoBloque bloqueDefecto; // puede ser null

    public String tipoNodo() { return "Elegir"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "ELEGIR");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        expresion.imprimir(nuevoPrefijo, false);
        for (int i = 0; i < casos.size(); i++) {
            casos.get(i).imprimir(nuevoPrefijo, bloqueDefecto == null && i == casos.size() - 1);
        }
        if (bloqueDefecto != null) {
            System.out.println(nuevoPrefijo + "└── DEFECTO");
            String prefDef = nuevoPrefijo + "    ";
            for (int i = 0; i < bloqueDefecto.sentencias.size(); i++) {
                bloqueDefecto.sentencias.get(i).imprimir(prefDef, i == bloqueDefecto.sentencias.size() - 1);
            }
        }
    }
}

class NodoCaso extends NodoAST {
    NodoAST valor;
    List<NodoAST> sentencias = new ArrayList<>();

    public String tipoNodo() { return "Caso"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "CASO");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        valor.imprimir(nuevoPrefijo, false);
        for (int i = 0; i < sentencias.size(); i++) {
            sentencias.get(i).imprimir(nuevoPrefijo, i == sentencias.size() - 1);
        }
    }
}

// ── Imprimir ──
class NodoImprimir extends NodoAST {
    NodoAST expresion;

    public String tipoNodo() { return "Imprimir"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "IMPRIMIR");
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        expresion.imprimir(nuevoPrefijo, true);
    }
}

// ── Leer ──
class NodoLeer extends NodoAST {
    String variable;

    public String tipoNodo() { return "Leer"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "LEER → " + variable);
    }
}

// ── Romper (break) ──
class NodoRomper extends NodoAST {
    public String tipoNodo() { return "Romper"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "ROMPER");
    }
}

// ── Expresión binaria ──
class NodoExpresionBinaria extends NodoAST {
    String operador;
    NodoAST izquierdo;
    NodoAST derecho;

    public String tipoNodo() { return "ExpBinaria"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "OP: " + operador);
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        izquierdo.imprimir(nuevoPrefijo, false);
        derecho.imprimir(nuevoPrefijo, true);
    }
}

// ── Expresión unaria ──
class NodoExpresionUnaria extends NodoAST {
    String operador;
    NodoAST operando;

    public String tipoNodo() { return "ExpUnaria"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "UNARIO: " + operador);
        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");
        operando.imprimir(nuevoPrefijo, true);
    }
}

// ── Número entero ──
class NodoNumeroEntero extends NodoAST {
    int valor;
    public String tipoNodo() { return "NumEntero"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "INT: " + valor);
    }
}

// ── Número decimal ──
class NodoNumeroDecimal extends NodoAST {
    double valor;
    public String tipoNodo() { return "NumDecimal"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "FLOAT: " + valor);
    }
}

// ── Cadena literal ──
class NodoCadenaLiteral extends NodoAST {
    String valor;
    public String tipoNodo() { return "CadenaLit"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "STR: \"" + valor + "\"");
    }
}

// ── Booleano ──
class NodoBooleano extends NodoAST {
    boolean valor;
    public String tipoNodo() { return "Booleano"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "BOOL: " + valor);
    }
}

// ── Identificador ──
class NodoIdentificador extends NodoAST {
    String nombre;
    public String tipoNodo() { return "Identificador"; }
    public void imprimir(String prefijo, boolean esUltimo) {
        String conector = esUltimo ? "└── " : "├── ";
        System.out.println(prefijo + conector + "ID: " + nombre);
    }
}
