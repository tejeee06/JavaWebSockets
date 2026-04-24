package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVIDOR DE CHAT MULTI-CLIENT
 * ==============================
 * Aquest servidor escolta connexions entrants al port 12345.
 * Cada vegada que un client es connecta, es crea un nou fil (thread)
 * per gestionar-lo de forma independent (comunicació simultània).
 *
 * Protocol: TCP/IP (orientat a connexió, fiable)
 * Port: 12345
 */
public class ServidorXat {

    // Port on escoltarà el servidor
    static final int PORT = 12345;

    // Llista compartida de tots els gestors de clients connectats
    // "static" perquè tots els fils hi han d'accedir
    static List<ServidorGestorDelsClients> clientsConnectats = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("=== SERVIDOR DE CHAT INICIANT ===");
        System.out.println("Escoltant al port: " + PORT);
        System.out.println("Esperant clients...");
        System.out.println("---------------------------------");

        // try-with-resources: tanca el ServerSocket automàticament en acabar
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Bucle infinit: el servidor mai para d'escoltar
            while (true) {

                // accept() BLOQUEJA fins que arriba un client
                Socket socketClient = serverSocket.accept();

                System.out.println("[+] Nou client connectat des de: "
                        + socketClient.getInetAddress().getHostAddress());

                // Creem un gestor per aquest client i l'arranquem en un fil nou
                ServidorGestorDelsClients gestor = new ServidorGestorDelsClients(socketClient);
                clientsConnectats.add(gestor);
                new Thread(gestor).start();  // <-- comunicació simultània!
            }

        } catch (IOException e) {
            System.err.println("[ERROR] No s'ha pogut iniciar el servidor: " + e.getMessage());
        }
    }

    /**
     * Envia un missatge a TOTS els clients connectats.
     * Es diu "broadcast" (difusió).
     *
     * @param missatge El text a enviar a tothom
     * @param origen   El GestorClient que ha enviat el missatge (per no enviar-li a ell mateix)
     */
    static synchronized void broadcast(String missatge, ServidorGestorDelsClients origen) {
        // "synchronized" evita problemes si dos fils envien alhora
        for (ServidorGestorDelsClients client : clientsConnectats) {
            if (client != origen) {          // no enviem al propi remitent
                client.enviarMissatge(missatge);
            }
        }
    }

    /**
     * Elimina un client de la llista quan es desconnecta.
     *
     * @param gestor El GestorClient a eliminar
     */
    static synchronized void eliminarClient(ServidorGestorDelsClients gestor) {
        clientsConnectats.remove(gestor);
    }
}
