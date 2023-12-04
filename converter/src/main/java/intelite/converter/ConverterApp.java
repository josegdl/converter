package intelite.converter;

import intelite.hilos.HiloCanal;
import intelite.models.Canal;
import intelite.models.Config;
import intelite.models.Control;
import intelite.util.HibernateUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.lang3.math.Fraction;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Desarrollo
 */
public class ConverterApp {

    private static final Logger LOG = LoggerFactory.getLogger(ConverterApp.class);
    private static final DateFormat DF = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss: ");
    public static final String DS = System.getProperty("file.separator").equals("\\") ? "\\" : "/";
    public static final boolean ISLINUX = DS.equals("/");
    public static final String FFMPEG_PATH = System.getProperty("user.dir") + DS;
    public static final String PROPERTIES_PATH = System.getProperty("user.dir") + DS;
    public static final String DIR_TMP = "TEMP" + DS;
    
    private TelegramNotifier telegramNotifier;

    private static final String CONTENT_CONFIG
            = "#### DESCOMENTAR LÍNEAS QUE INICIEN CON #> (QUITAR # y >) #####\n\n"
            + "# ********** CONFIGURACIÓN GENERAL **********\n"
            + "\n"
            + "# ----- Formato de salida -----\n"
            + "# Valores: mp4 y wmv\n"
            + "# Descripción: establece el formato de salida de la conversión (valor por defecto: mp4)\n"
            + ">fto=mp4\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Número de intentos de pegado -----\n"
            + "# Valores: un valor entero mayor a 0 (valor sugerido: 120)\n"
            + "# Descripción: indica el número de intentos de pegado que haré el programa (valor por defecto: 120)\n"
            + ">nint=120\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Frecuencia intento de pegado (en milisegundos) -----\n"
            + "# Valores: un valor entero mayor a 0 (valore sugerido: 500)\n"
            + "# Descripción: indica la frecuencia de intento de pegado en milisegundos (valor por defecto: 500)\n"
            + ">fint=500\n"
            + "# -------------------------\n"
            + "\n"
            + "# nint x frec = 120 * 500 = 60000 milisegundos = 120 intentos de pegado en 1 minuto.\n"
            + "\n"
            + "# ----- Custom Settings  -----\n"
            + "# Valores: on y off\n"
            + "# Descripción: indica si para la conversión se tomarán las propiedades de origen o los ajustes personalizados de audio y video (valor por defecto: off)\n"
            + ">custom=off\n"
            + "\n"
            + "# ********** CONFIGURACIÓN DE VIDEO **********\n"
            + "\n"
            + "# ----- Resolución -----\n"
            + "# Valores: 640x480, 720x576, 1280x720, 1920x1080\n"
            + "# Descripción: establece la resolución de salida del video (valor por defecto: 1280x720)\n"
            + ">res=1280x720\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Bitrate video (kbps) -----\n"
            + "# Valores: un valor entero (valor sugerido entre 100 y 1000)\n"
            + "# Descripción: establece el bitrate del video (valor por defecto: 750)\n"
            + ">vbr=750\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Fotogramas por segundo (fps) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 24, 25, 30, 60)\n"
            + "# Descripción: indica el valor de la propiedad fps del video (valor por defecto: 30)\n"
            + ">fps=30\n"
            + "# -------------------------\n"
            + "\n"
            + "# ********** CONFIGURACIÓN DE AUDIO **********\n"
            + "\n"
            + "# ----- Samplerate audio (Hz) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 32000, 44100, 48000)\n"
            + "# Descripción: indica la velocidad de muestra de sonido (valor por defecto: 32000)\n"
            + ">asr=32000\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Bitrate audio (kbps) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 48, 64, 96, 128, 160, 192, 256)\n"
            + "# Descripción: establece el bitrate del audio (valor por defecto: 96)\n"
            + ">abr=96\n"
            + "# -------------------------\n"
            + "\n"
            + "\n\n";

    private static final String CONTENT_CANALES
            = "# ********** CONFIGURACIÓN DE CANALES A CONVERTIR **********\n"
            + "\n"
            + "# ===== Campos que conforman el registro de cada canal: =====\n"
            + "# CANAL: establece el nombre del canal, debe contener solo letras, números y/o guiones bajos o medios (p. ej. 34-2). Este campo no puede estar vacio.\n"
            + "# ALIAS: establece el alias del canal, usado para crear la carpeta del canal, debe contener solo letras, números y/o guiones bajos o medios (p. ej. TV34-2b). Este campo no puede estar vacio.\n"
            + "# ORIGEN: indica el origen del archivo a convertir (p. ej. C:\\\\Users\\\\Usuario\\\\GrabacionesTV\\\\TV34-2b). Este campo no puede estar vacio.\n"
            + "# DESTINO: indica el directorio de destino del archivo convertido (p. ej. SO WINDOWS=C:\\\\Users\\\\Usuario\\\\GrabacionesTV, SO LINUX=/opt/GrabacionesTV). Este campo no puede estar vacio.\n"
            + "# ACTIVO: indica si el canal está activo para convertirse, los valores pueden 0 (inactivo) y 1 (activo). Si no se indica un valor, el valor por defecto será 1 (activo). \n"
            + "# DELAY: indica el tiempo, en segundos, de espera para que el hilo del canal vuelva a buscar archivos para convertir. Si no se indica un valor, el valor por defecto será 60 (un minuto).\n"
            + "\n"
            + "# ===== Formato para registrar un canal: =====\n"
            + "# CANAL=ALIAS, ORIGEN, DESTINO, ACTIVO, DELAY\n"
            + "\n"
            + "# ===== Ejemplo de registro de un canal en SO Windows: =====\n"
            + "# 34-2=TV34-2b, C:\\\\Users\\\\Usuario\\\\GrabacionesTV\\\\TV34-2b, C:\\\\Users\\\\Usuario\\\\GrabacionesMP4, 1, 60\n"
            + "\n"
            + "# ===== Ejemplo de registro de un canal en SO Linux: =====\n"
            + "# 34-2=TV34-2b, /opt/GrabacionesTV/TV34-2b, /opt/GrabacionesMP4, 1, 60\n"
            + "\n"
            + "\n"
            + "# /////////////// REGISTRO DE CANALES A CONVERTIR ///////////////";

    public static void main(String args[]) {
//        LOG.error("===== CONVERTER v.20210708 =====\n");
//        LOG.error("===== CONVERTER v.20211230 =====\n");
          LOG.error("===== CONVERTER v.20231130 =====\n");

        Properties conf = new Properties();
        Properties canales = new Properties();

        TelegramNotifier telegramNotifier = new TelegramNotifier("6659796544:AAFcsEWY_SziJr8Y7DHtVOrOcPO1e_c0_Ko","-1001869238197");
        
        String botToken = "6659796544:AAFcsEWY_SziJr8Y7DHtVOrOcPO1e_c0_Ko";
        String chatId = "-1001869238197";

        // Llama al método sendMessage
        sendMessage(botToken, chatId, "Converter.");
        
        String path_conf = PROPERTIES_PATH + "config1.properties";
        File conf_file = new File(path_conf);
        if (!conf_file.exists()) {
            System.out.println(DF.format(new Date()) + "Creando archivo config.properties...");
            Properties p = new Properties();
            try {
                p.store(new FileWriter("config1.properties"), CONTENT_CONFIG);
            } catch (IOException e) {
                LOG.error("No se pudo crear el archivo 'config1.properties' en la ruta " + path_conf, e);
            }
        }

        String path_canales = PROPERTIES_PATH + "canales1.properties";
        File canales_file = new File(path_canales);
        if (!canales_file.exists()) {
            System.out.println(DF.format(new Date()) + "Creando archivo canales1.properties...");
            Properties p = new Properties();
            try {
                p.store(new FileWriter("canales1.properties"), CONTENT_CANALES);
            } catch (IOException e) {
                LOG.error("No se pudo crear el archivo 'canales1.properties' en la ruta " + path_canales, e);
            }
        }

        try {
            conf.load(new FileReader(path_conf));
            canales.load(new FileReader(path_canales));
            checkTablaControl();
            
            
            iniciarConversion(conf, canales, telegramNotifier);
            
            
        } catch (FileNotFoundException e) {
            LOG.error("No se pudo encontrar o cargar alguno de los archivos de configuración!", e);
            close(telegramNotifier);
        } catch (IOException e) {
            LOG.error("No se pudo abrir alguno de los archivos de configuración!", e);
            close(telegramNotifier);
        }
    }

    private static void iniciarConversion(Properties conf, Properties canales, TelegramNotifier telegramNotifier) {
        String fto = conf.getProperty("fto", "mp4").toLowerCase();

        String nint_s = conf.getProperty("nint", "120");
        Integer nint = Integer.parseInt(nint_s);

        String fint_s = conf.getProperty("fint", "500");
        Integer fint = Integer.parseInt(fint_s);

        String custom = conf.getProperty("custom", "off").toLowerCase();

        String res_s = conf.getProperty("res", "1280x720");
        String[] txt = res_s.split("x");
        Integer res_w = Integer.parseInt(txt[0]);
        Integer res_h = Integer.parseInt(txt[1]);

        String vbr_s = conf.getProperty("vbr", "750");
        Long vbr = Long.parseLong(vbr_s) * 1000;

        String fps_s = conf.getProperty("fps", "30");
        Fraction fps = Fraction.getFraction(Integer.parseInt(fps_s), 1);

        String asr_s = conf.getProperty("asr", "32000");
        Integer asr = Integer.parseInt(asr_s);

        String abr_s = conf.getProperty("abr", "96");
        Long abr = Long.parseLong(abr_s) * 1000;

        // Se crea el objeto de tipo Config
        Config config = new Config(fto, nint, fint, custom, res_w, res_h, vbr, fps, asr, abr);

        System.out.println("\n*** CONFIGURACIÓN ***");
        System.out.println("> Formato de salida conversión: " + fto);
        System.out.println("> Número de intentos de pegado: " + nint_s);
        System.out.println("> Frecuencia intento de pegado: " + fint_s);
        System.out.println("> Custom settings: " + custom.toUpperCase());

        if (custom.equals("off")) {
            System.out.println("> Audio: configuración de origen");
            System.out.println("> Video: configuración de origen");
        } else {
            System.out.println("> Resolución: " + res_s);
            System.out.println("> Bitrate video: " + vbr_s + " kbps");
            System.out.println("> Fotogramas por segundo: " + fps_s + " fps");
            System.out.println("> Samplerate audio: " + asr_s + " Hz");
            System.out.println("> Bitrate audio: " + abr_s + " kbps");
        }
        System.out.println("");

        System.out.println(DF.format(new Date()) + "Inicia obtención de canales a convertir...");
        telegramNotifier.sendMessage("Inicia obtencion de canales a convertir");
        Enumeration<Object> keys = canales.keys();
        if (keys.hasMoreElements()) {
            // Se crea el directorio temporal si es que aún no existe 
            File carpeta_tmp = new File(DIR_TMP);
            Boolean bseguir = true;
            if (!carpeta_tmp.exists()) {
                // Si no existe, se crea la carpeta de salida
                if (ISLINUX) {
//                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx"); // 777
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x"); // 755
                    try {
                        Files.createDirectories(Paths.get(DIR_TMP), PosixFilePermissions.asFileAttribute(perms));
                        System.out.println(DF.format(new Date()) + "SE HA CREADO LA CARPETA TEMPORAL -> " + DIR_TMP);
                    } catch (IOException e) {
                        bseguir = false;
                        LOG.warn("ERROR AL CREAR LA CARPETA TEMPORAL -> " + DIR_TMP);
                        LOG.warn("Error al crear la carpeta temporal > iniciarConversion:", e);
                    }
                } else {
                    if (carpeta_tmp.mkdirs()) {
                        System.out.println(DF.format(new Date()) + "SE HA CREADO LA CARPETA TEMPORAL -> " + carpeta_tmp.getAbsolutePath());
                    } else {
                        bseguir = false;
                        LOG.info("ERROR AL CREAR LA CARPETA TEMPORAL -> " + carpeta_tmp.getAbsolutePath());
                    }
                }
            }

            if (bseguir) {
                // Se crean y ejecutan los hilos de los canales activos
                while (keys.hasMoreElements()) {
                    
                    Object key = keys.nextElement();
                    String[] d = canales.get(key).toString().trim().replace(" ", "").split(",");
                    String nombre = key.toString();
                    String alias = d[0];
                    String origen = setBackslash(d[1]);
                    String destino = setBackslash(d[2]);
                    Integer activo = d.length > 3 ? Integer.parseInt(d[3]) : 1;
                    Integer delay = d.length > 4 ? Integer.parseInt(d[4]) : 60;

                    // Se crea el objeto canal respectivo
                    Canal canal = new Canal(nombre, alias, origen, destino, activo, delay);
                    // Creación del hilo del canal
                    if (canal.getActivo().equals(1)) {
                        System.out.println(DF.format(new Date()) + "Creando hilo canal " + nombre + "...");
                        
                        Timer timer = new Timer();
                        // Programar el cierre de la aplicación
                        timer.scheduleAtFixedRate(new TimerTask() {
                            public void run() {
                                telegramNotifier.sendMessage("Cerrar la aplicacion para el canal " + nombre + "....");
                                System.out.println("Cerrando la aplicación para el canal " + nombre + "...");
                                cerrarAplicacion(conf);
                                System.exit(0);
                            }
                            private void cerrarAplicacion(Properties conf) {
                                    try {
                                        String sistemaOperativo = System.getProperty("os.name").toLowerCase();

                                        // Comando por defecto para sistemas basados en Windows
                                        String comandoCierre = "taskkill /F /IM ConverterBD.exe";

                                        // Ajustar el comando para otros sistemas operativos si es necesario
                                        if (sistemaOperativo.contains("nix") || sistemaOperativo.contains("nux") || sistemaOperativo.contains("mac")) {
                                            // Comando para sistemas basados en Unix/Linux/Mac
                                            comandoCierre = "pkill -f ConverterBD";
                                        }
                                        Process proceso = Runtime.getRuntime().exec(comandoCierre);
                                        proceso.waitFor();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            },  2 * 60 * 60 * 1000, 2 * 60 * 60 * 1000);// Cada 2 horas 
                        
                        HiloCanal hilo_canal = new HiloCanal("Hilo " + nombre, canal, config, telegramNotifier);
                        System.out.println(DF.format(new Date()) + hilo_canal.getName() + " creado!");
                        System.out.println(DF.format(new Date()) + "Ejecutando proceso de conversión del canal " + nombre + "...");
                        hilo_canal.start();
                    }
                }
            }
        } else {
            LOG.info("No hay canales registrados o activos para convertir en el archivo canales1.properties!");
            telegramNotifier.sendMessage("No hay canales registrados o activos para convertir en el archivo canales1.properties!");
        }
    }

    private static String setBackslash(String cadena) {
        if (cadena != null && !cadena.equals("")) {
            int length = cadena.length();
            String backslash = cadena.substring(length - 1);
            if (!backslash.equals(DS)) {
                cadena += DS;
            }
        }
        return cadena;
    }

    private static void checkTablaControl() {
        // Comprueba si el registro de control es mayor a 500 registros y los borra
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            List<Control> ctrl = session.createQuery("FROM Control", Control.class).getResultList();
            ctrl.forEach((c) -> {
                System.out.println(c);
            });
            if (ctrl.size() > 1500) {
                session.createSQLQuery("TRUNCATE TABLE Control").executeUpdate();
                session.createSQLQuery("ALTER TABLE Control ALTER COLUMN id RESTART WITH 1").executeUpdate();
            }
            transaction.commit();
            session.close();
        } catch (Exception e) {
            LOG.warn("Error al borrar los registros de control!", e);
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    private static void close(TelegramNotifier telegramNotifier) {
        LOG.debug("Enviando Mensaje y Terminando ejecución Converter...");
        
        telegramNotifier.sendMessage("Terminando ejecucion Converter...");
        // Espera un tiempo para asegurar que el mensaje se envíe antes de salir
        try {
            Thread.sleep(2000); // Puedes ajustar el tiempo de espera según sea necesario
        } catch (InterruptedException e) {
        System.out.println(DF.format(new Date()) + "Terminando ejecución Converter...");
        System.exit(0);
        }
    }
    
    private static void sendMessage(String botToken, String chatId, String messageText) {
        // URL del punto final de la API de Telegram para enviar mensajes
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        try {
            // Configuración de la conexión HTTP
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Creación del cuerpo del mensaje en formato JSON
            String mensaje = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + messageText + "\"}";

            // Envío de la solicitud con el cuerpo del mensaje
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = mensaje.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Lectura de la respuesta
            int responseCode = connection.getResponseCode();
            System.out.println("Código de respuesta: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
