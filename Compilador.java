import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Compilador {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║          COMPILADOR - LENGUAJE EN ESPAÑOL v1.0                ║");
        System.out.println("║          Universidad Politécnica de Chiapas                   ║");
        System.out.println("║          Compiladores e Intérpretes - Proyecto Final           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        // ── 1. Leer archivo fuente ──
        String archivoFuente = "programa.txt";
        if (args.length > 0) {
            archivoFuente = args[0];
        }

        String codigoFuente;
        try {
            codigoFuente = new String(Files.readAllBytes(Paths.get(archivoFuente)));
        } catch (IOException e) {
            System.err.println("\n[ERROR] No se pudo leer el archivo: " + archivoFuente);
            System.err.println("Uso: java Compilador <archivo_fuente.txt>");
            return;
        }

        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ CÓDIGO FUENTE: " + archivoFuente);
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        // Mostrar código con números de línea
        String[] lineas = codigoFuente.split("\n");
        for (int i = 0; i < lineas.length; i++) {
            System.out.printf("  %3d │ %s\n", i + 1, lineas[i]);
        }

        boolean hayErrores = false;

        // ══════════════════════════════════════════════════
        // FASE 1: ANÁLISIS LÉXICO
        // ══════════════════════════════════════════════════
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  FASE 1: ANÁLISIS LÉXICO");
        System.out.println("══════════════════════════════════════════════════════════════");

        Lexer lexer = new Lexer(codigoFuente);
        List<Token> tokens = lexer.escanear();

        System.out.println("\n  Tokens encontrados: " + (tokens.size() - 1)); // -1 por EOF
        System.out.println();
        for (Token t : tokens) {
            if (t.tipo != Token.Tipo.EOF) {
                System.out.println("  " + t);
            }
        }

        lexer.mostrarTablaSimbolos();

        if (!lexer.getErrores().isEmpty()) {
            hayErrores = true;
            System.out.println("\n  ⚠ ERRORES LÉXICOS:");
            for (String err : lexer.getErrores()) {
                System.out.println("    " + err);
            }
        } else {
            System.out.println("\n  ✓ Análisis léxico completado sin errores");
        }

        // ══════════════════════════════════════════════════
        // FASE 2: ANÁLISIS SINTÁCTICO
        // ══════════════════════════════════════════════════
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  FASE 2: ANÁLISIS SINTÁCTICO");
        System.out.println("══════════════════════════════════════════════════════════════");

        Parser parser = new Parser(tokens);
        NodoPrograma ast = parser.parsear();

        System.out.println("\n  ÁRBOL SINTÁCTICO ABSTRACTO (AST):");
        System.out.println();
        ast.imprimir("  ", false);

        if (!parser.getErrores().isEmpty()) {
            hayErrores = true;
            System.out.println("\n  ⚠ ERRORES SINTÁCTICOS:");
            for (String err : parser.getErrores()) {
                System.out.println("    " + err);
            }
        } else {
            System.out.println("\n  ✓ Análisis sintáctico completado sin errores");
        }

        // ══════════════════════════════════════════════════
        // FASE 3: ANÁLISIS SEMÁNTICO
        // ══════════════════════════════════════════════════
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  FASE 3: ANÁLISIS SEMÁNTICO");
        System.out.println("══════════════════════════════════════════════════════════════");

        AnalizadorSemantico semantico = new AnalizadorSemantico();
        semantico.analizar(ast);
        semantico.mostrarTablaSemantica();

        if (!semantico.getAdvertencias().isEmpty()) {
            System.out.println("\n  ⚠ ADVERTENCIAS:");
            for (String adv : semantico.getAdvertencias()) {
                System.out.println("    " + adv);
            }
        }

        if (!semantico.getErrores().isEmpty()) {
            hayErrores = true;
            System.out.println("\n  ⚠ ERRORES SEMÁNTICOS:");
            for (String err : semantico.getErrores()) {
                System.out.println("    " + err);
            }
        } else {
            System.out.println("\n  ✓ Análisis semántico completado sin errores");
        }

        // ══════════════════════════════════════════════════
        // FASE 4: GENERACIÓN DE CÓDIGO INTERMEDIO
        // ══════════════════════════════════════════════════
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  FASE 4: GENERACIÓN DE CÓDIGO INTERMEDIO");
        System.out.println("══════════════════════════════════════════════════════════════");

        GeneradorCodigoIntermedio genIntermedio = new GeneradorCodigoIntermedio();
        genIntermedio.generar(ast);
        genIntermedio.mostrarCuadruplas();

        System.out.println("\n  ✓ Código intermedio generado: " + genIntermedio.getCuadruplas().size() + " cuádruplas");

        // ══════════════════════════════════════════════════
        // FASE 5: GENERACIÓN DE CÓDIGO OBJETO
        // ══════════════════════════════════════════════════
        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  FASE 5: GENERACIÓN DE CÓDIGO OBJETO");
        System.out.println("══════════════════════════════════════════════════════════════");

        GeneradorCodigoObjeto genObjeto = new GeneradorCodigoObjeto(genIntermedio.getCuadruplas());
        genObjeto.generar();
        genObjeto.mostrarCodigoObjeto();

        // Guardar código objeto en archivo
        try {
            String nombreObjeto = archivoFuente.replace(".txt", "") + ".obj";
            PrintWriter pw = new PrintWriter(new FileWriter(nombreObjeto));
            for (String instr : genObjeto.getInstrucciones()) {
                pw.println(instr);
            }
            pw.close();
            System.out.println("\n  ✓ Código objeto guardado en: " + nombreObjeto);
            System.out.println("  ✓ Total instrucciones: " + genObjeto.getInstrucciones().size());
        } catch (IOException e) {
            System.err.println("  [ERROR] No se pudo guardar el código objeto: " + e.getMessage());
        }

        // ══════════════════════════════════════════════════
        // FASE 6: EJECUCIÓN
        // ══════════════════════════════════════════════════
        if (!hayErrores) {
            System.out.println("\n══════════════════════════════════════════════════════════════");
            System.out.println("  FASE 6: EJECUCIÓN DEL PROGRAMA");
            System.out.println("══════════════════════════════════════════════════════════════");

            MaquinaVirtual vm = new MaquinaVirtual(genObjeto.getInstrucciones());
            vm.ejecutar();
        } else {
            System.out.println("\n══════════════════════════════════════════════════════════════");
            System.out.println("  ⚠ NO SE PUEDE EJECUTAR: Se encontraron errores");
            System.out.println("══════════════════════════════════════════════════════════════");
        }

        // ── Resumen final ──
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESUMEN DE COMPILACIÓN                     ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Tokens generados:        %-34d ║\n", tokens.size() - 1);
        System.out.printf("║  Cuádruplas generadas:    %-34d ║\n", genIntermedio.getCuadruplas().size());
        System.out.printf("║  Instrucciones objeto:    %-34d ║\n", genObjeto.getInstrucciones().size());
        System.out.printf("║  Errores léxicos:         %-34d ║\n", lexer.getErrores().size());
        System.out.printf("║  Errores sintácticos:     %-34d ║\n", parser.getErrores().size());
        System.out.printf("║  Errores semánticos:      %-34d ║\n", semantico.getErrores().size());
        System.out.printf("║  Estado:                  %-34s ║\n", hayErrores ? "CON ERRORES" : "COMPILACIÓN EXITOSA");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
}
