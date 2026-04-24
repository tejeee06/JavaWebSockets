package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * VERIFICADOR DE DISPONIBILITAT
 * ==============================
 * Eina per comprovar si el servidor de chat està en marxa (criteri 1.7).
 * Intenta obrir una connexió TCP al servidor.
 * Si s'obre correctament → el servei està disponible.
 * Si no → el servei no respon.
 *
 * Útil per:
 *   - Monitoratge del servei
 *   - Depuració (comprovar que el servidor s'ha arrencat)
 *   - Verificació abans de connectar el client
 */
public class ClientVerificadorDisponibilitatDelServidor {

    static final String ADRECA = "localhost";
    static final int PORT = 12345;
    static final int TIMEOUT_MS = 2000; // Temps màxim d'espera: 2 segons

    public static void main(String[] args) {

        System.out.println("=== VERIFICADOR DE DISPONIBILITAT ===");
        System.out.printf("Comprovant %s:%d ...%n", ADRECA, PORT);

        if (estaDisponible(ADRECA, PORT)) {
            System.out.println("✓ SERVEI DISPONIBLE - El servidor de chat està en marxa.");
        } else {
            System.out.println("✗ SERVEI NO DISPONIBLE - El servidor no respon.");
            System.out.println("  → Comprova que has arrencat ServidorChat.java");
        }
    }

    /**
     * Comprova si hi ha un servei escoltant a l'adreça i port indicats.
     *
     * @param adreca L'adreça IP o hostname del servidor
     * @param port   El port TCP a verificar
     * @return true si el servei respon, false si no
     */
    public static boolean estaDisponible(String adreca, int port) {
        // Creem un socket sense connectar, amb timeout
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(adreca, port), TIMEOUT_MS);
            return true; // Si hem pogut connectar, el servei està disponible
        } catch (IOException e) {
            return false; // No s'ha pogut connectar
        }
    }
}
