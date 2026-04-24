package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * GESTOR DE CLIENT (fil per a cada usuari)
 * ==========================================
 * Aquesta classe s'executa en un fil independent per a cada client.
 * Implementa Runnable per poder usar-la amb Thread.
 *
 * Responsabilitats:
 *  - Rebre missatges del client
 *  - Enviar missatges al client
 *  - Fer broadcast quan arriba un missatge nou
 *  - Gestionar la desconnexió
 */
public class ServidorGestorDelsClients implements Runnable {

    private Socket socket;         // Connexió amb el client
    private BufferedReader entrada; // Per llegir el que envia el client
    private PrintWriter sortida;   // Per escriure al client
    private String nomUsuari;      // Nom del client (se li demana en connectar)

    /**
     * Constructor: rep el socket del client i prepara els fluxos d'entrada/sortida.
     *
     * @param socket La connexió TCP amb el client
     */
    public ServidorGestorDelsClients(Socket socket) {
        this.socket = socket;
        try {
            // InputStreamReader converteix bytes -> caràcters
            // BufferedReader afegeix eficiència llegint per línies
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // PrintWriter amb "true" fa que cada println() s'enviï immediatament (autoFlush)
            sortida = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            System.err.println("[ERROR] No s'han pogut crear els fluxos: " + e.getMessage());
        }
    }

    /**
     * run() s'executa quan arranquem el fil (new Thread(this).start()).
     * Conté el bucle principal de comunicació.
     */
    @Override
    public void run() {

        try {
            // 1) Demanem el nom d'usuari
            sortida.println("Benvingut/da al chat! Escriu el teu nom:");
            nomUsuari = entrada.readLine();

            // Avisem la resta que ha entrat algú
            System.out.println("[+] L'usuari '" + nomUsuari + "' s'ha unit al chat.");
            ServidorXat.broadcast("*** " + nomUsuari + " s'ha unit al chat ***", this);
            sortida.println("Connectat/da! Ja pots escriure missatges.");

            // 2) Bucle principal: llegim missatges fins que el client escrigui "sortir"
            String missatgeRebut;
            while ((missatgeRebut = entrada.readLine()) != null) {

                if (missatgeRebut.equalsIgnoreCase("sortir")) {
                    break; // surt del bucle i desconnecta
                }

                // Format del missatge: [Nom]: text
                String missatgeFormatat = "[" + nomUsuari + "]: " + missatgeRebut;
                System.out.println(missatgeFormatat);

                // Enviem a tots els altres
                ServidorXat.broadcast(missatgeFormatat, this);
            }

        } catch (IOException e) {
            // Pot passar si el client tanca la finestra sense avisar
            System.out.println("[-] Connexió perduda amb: " + nomUsuari);
        } finally {
            // 3) Neteja: sempre s'executa, tant si hi ha error com si no
            desconnectar();
        }
    }

    /**
     * Envia un missatge de text al client.
     * Mètode cridat des d'altres fils (broadcast).
     *
     * @param missatge El text a enviar
     */
    public void enviarMissatge(String missatge) {
        sortida.println(missatge);
    }

    /**
     * Tanca la connexió i neteja els recursos.
     * Elimina el client de la llista global.
     */
    private void desconnectar() {
        try {
            ServidorXat.eliminarClient(this);
            if (nomUsuari != null) {
                ServidorXat.broadcast("*** " + nomUsuari + " ha sortit del chat ***", this);
                System.out.println("[-] L'usuari '" + nomUsuari + "' s'ha desconnectat.");
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Tancant socket: " + e.getMessage());
        }
    }
}
