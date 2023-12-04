package intelite.hilos;

import intelite.converter.ConverterApp;
import static intelite.converter.ConverterApp.DS;
import static intelite.converter.ConverterApp.FFMPEG_PATH;
import static intelite.converter.ConverterApp.ISLINUX;
import intelite.converter.TelegramNotifier;
import intelite.models.Canal;
import intelite.models.Config;
import intelite.models.Control;
import intelite.util.HibernateUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HiloCanal extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(HiloCanal.class);
    private static final DateFormat DF = new SimpleDateFormat("dd/MM/yyyy-HH:mm:ss: ");
    private final DateFormat df_anio = new SimpleDateFormat("yyyy");
    private final DateFormat df_mes = new SimpleDateFormat("MM");
    private final DateFormat df_dia = new SimpleDateFormat("dd");
    private Canal canal;
    private Config config;
    
    private TelegramNotifier telegramNotifier;
    
    CopyOption[] options_move = new CopyOption[]{
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
    };
//    CopyOption[] options_copy = new CopyOption[]{
//        StandardCopyOption.REPLACE_EXISTING,
//        StandardCopyOption.COPY_ATTRIBUTES
//    };

    public HiloCanal() {
        super();
    }

    public HiloCanal(String name, Canal canal, Config config, TelegramNotifier telegramNotifier) {
        super(name);
        this.canal = canal;
        this.config = config;
        this.telegramNotifier= telegramNotifier;
    }

    @Override
    public void run() {
        //Envia una notificacion al inicio
        telegramNotifier.sendMessage("El canal "+ canal.getNombre() + " ha iniciado. ");
        while (!Thread.interrupted()) {
            startConvert();
        }
    }

    private void verificarConversion(String filein, String fileout, Path path_out_tmp, Path path_out) {
        try {
            File originalFile = new File(filein);
            File convertedFile = new File(fileout);

            // Verificar si los archivos originales y convertidos existen
            if (originalFile.exists() && convertedFile.exists()) {
                // Verificar si los tamaños son iguales
                if (originalFile.length() == convertedFile.length()) {
                    System.out.println(DF.format(new Date()) + "CONVERSIÓN EXITOSA -> " + fileout);
                    // Eliminar el archivo original
                    if (originalFile.delete()) {
                        System.out.println(DF.format(new Date()) + "ARCHIVO ORIGINAL ELIMINADO -> " + filein);
                    } else {
                        LOG.warn("No se pudo eliminar el archivo original -> " + filein);
                    }
                } else {
                    LOG.warn("CONVERSIÓN FALLIDA -> " + fileout);
                    telegramNotifier.sendMessage("CONVERSION FALLIDA -> " + fileout);
                    // Intentar nuevamente la conversión
                    System.out.println(DF.format(new Date()) + "INTENTANDO NUEVAMENTE LA CONVERSIÓN -> " + filein);
                    telegramNotifier.sendMessage("INTENTANDO NUEVAMENTE LA CONVERSION -> " + filein);
                    Files.move(path_out, path_out_tmp, options_move);
                    startConvert(); // Volver a iniciar la conversión
                }
            } else {
                LOG.warn("No se encontraron archivos originales o convertidos -> " + filein + ", " + fileout);
                telegramNotifier.sendMessage("No se encontraron archivos originales o convertidos -> " + filein + ", " + fileout);
            }
        } catch (Exception e) {
            LOG.error("Error al verificar la conversión -> " + filein, e);
            telegramNotifier.sendMessage("Error al verificar la conversion ->" + filein);
        }
}

    
    private void startConvert() {
        // Obtener archivos mkv de hoy
        Calendar today = Calendar.getInstance();
        List<String[]> files_today = getListFiles(today.getTime());

        // Obtener archivos mkv de ayer
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        List<String[]> files_yesterday = getListFiles(yesterday.getTime());

        List<String[]> files_convert = new ArrayList<>();
        files_convert.addAll(files_today);
        files_convert.addAll(files_yesterday);
        if (files_convert.isEmpty()) {
            System.out.println(DF.format(new Date()) + "Esperando archivos para convertir del canal " + canal.getNombre() + "...");
        } else {
            files_convert.forEach((data_file) -> {
                String input = data_file[0];
                String out_tmp = data_file[1];
                String out = data_file[2];
                String filein = data_file[3];
                String fileout = data_file[4];
                String convertir = data_file[5];

                Path path_out_tmp = Paths.get(out_tmp);
                Path path_out = Paths.get(out);
                
               
                    if (convertir.equals("true")) {
                        // CONVERTIR y MOVER el archivo temporal
                        try {
                            FFmpegBuilder builder;
                            if (config.getCustom().equals("off")) {
                                switch (config.getFto()) {
                                    case "wmv":
                                        builder = new FFmpegBuilder()
                                                .setInput(input) // Filename, or a FFmpegProbeResult
                                                .overrideOutputFiles(true) // Override the output if it exists
                                                .addOutput(out_tmp) // Filename for the destination
                                                // AUDIO
                                                //                                            .addExtraArgs("-q:a", "2")
                                                .setAudioCodec("wmav2") // -codec:a or -acodec
                                                //                                            .setAudioChannels(FFmpeg.AUDIO_STEREO) // Stereo audio
                                                // VIDEO
                                                //                                            .addExtraArgs("-q:v", "2")
                                                .setVideoCodec("wmv2") // -codec:v or -vcodec msmpeg4-wmv2
                                                .done();
                                        break;
                                    default: // mp4
                                        builder = new FFmpegBuilder()
                                                .setInput(input) // Filename, or a FFmpegProbeResult
                                                .overrideOutputFiles(true) // Override the output if it exists
                                                .addOutput(out_tmp) // Filename for the destination
                                                // Se copian los codecs de audio y video (para no recodificar)
                                                .addExtraArgs("-c", "copy")
                                                .done();
                                        break;
                                }
                            } else {
                                switch (config.getFto()) {
                                    case "wmv":
                                        builder = new FFmpegBuilder()
                                                .setInput(input) // Filename, or a FFmpegProbeResult
                                                .overrideOutputFiles(true) // Override the output if it exists
                                                .addOutput(out_tmp) // Filename for the destination
                                                // AUDIO
                                                //                                            .addExtraArgs("-q:a", "2")
                                                .setAudioChannels(FFmpeg.AUDIO_STEREO) // Stereo audio
                                                .setAudioBitRate(config.getAbr()) // audio bitrate to be exact 128kbit
                                                .setAudioSampleRate(config.getAsr()) // audio sampling frequency.
                                                .setAudioCodec("wmav2") // -codec:a or -acodec
                                                // VIDEO
                                                //                                            .addExtraArgs("-q:v", "2")
                                                .setVideoResolution(config.getRes_w(), config.getRes_h())
                                                .setVideoBitRate(config.getVbr())
                                                .setVideoFrameRate(config.getFps())
                                                .setVideoCodec("wmv2") // -codec:v or -vcodec msmpeg4-wmv2
                                                .done();
                                        break;
                                    default: // mp4
                                        builder = new FFmpegBuilder()
                                                .setInput(input) // Filename, or a FFmpegProbeResult
                                                .overrideOutputFiles(true) // Override the output if it exists
                                                .addOutput(out_tmp) // Filename for the destination
                                                // AUDIO 
                                                .setAudioChannels(FFmpeg.AUDIO_STEREO) // Stereo audio
                                                .setAudioBitRate(config.getAbr()) // audio bitrate to be exact 128kbit
                                                .setAudioSampleRate(config.getAsr()) // audio sampling frequency.
                                                // VIDEO
                                                .setVideoResolution(config.getRes_w(), config.getRes_h())
                                                .setVideoBitRate(config.getVbr())
                                                .setVideoFrameRate(config.getFps())
                                                .done();
                                        break;
                                }
                            }
                            FFmpeg ffmpeg = new FFmpeg(FFMPEG_PATH + "cffmpeg");
                            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

                            System.out.println(DF.format(new Date()) + "CONVIRTIENDO archivo " + filein + " ---> " + fileout + "...");

                            // Run a one-pass encode
                            executor.createJob(builder).run();
                            // Or run a two-pass encode (which is better quality at the cost of being slower)
    //                        executor.createTwoPassJob(builder).run();

                            System.out.println(DF.format(new Date()) + "ARCHIVO TEMPORAL CREADO -> " + out_tmp);

                            if (ISLINUX) {
                                // Se asigana permisos al archivo temporal mp4
                                File file_out_tmp = new File(out_tmp);
                                if (file_out_tmp.exists() && file_out_tmp.isFile()) {
                                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
                                    Files.setPosixFilePermissions(file_out_tmp.toPath(), perms);
                                }
                            }
                            // MOVER Y SOBREESCRIBIR (si existe) el archivo de destino
                            moverTemporal(path_out_tmp, path_out, out);

                        } catch (IOException e) {
                            LOG.warn("IOException startConvert:", e);
                        }

                    } else {
                        // MOVER Y SOBREESCRIBIR el archivo de destino (Se verifica que siga existiendo el archivo temporal)
                        File file_out_tmp = new File(out_tmp);
                        if (file_out_tmp.exists() && file_out_tmp.isFile()) {
                            moverTemporal(path_out_tmp, path_out, out);
                            verificarConversion(filein, fileout, path_out_tmp, path_out);// Llamar a la funcion de verificacion 
                        }
                }
                try {
                    HiloCanal.sleep(3000); // Tiempo de espera antes de convertir el archivo que sigue en la cola
                } catch (InterruptedException e) {
                    LOG.warn("Exepción en tiempo de espera canal " + canal.getNombre() + ":", e);
                    Thread.currentThread().interrupt();
                }
            });// FIN ForEach
          
    }
        try {
            int delay = 60; // 60 segundos por defecto para todos los canales
            if (canal.getDelay() != null) {
                delay = canal.getDelay(); // Delay establecido para el canal
            }
            HiloCanal.sleep(delay * 1000); // Tiempo (en milisegundos) de espera antes de volver a buscar archivos para convertir

        } catch (InterruptedException e) {
            LOG.warn("Exepción en delay canal " + canal.getNombre() + ":", e);
            Thread.currentThread().interrupt();
        }
    }

    private List<String[]> getListFiles(Date date) {
        if (canal.getOrigen() != null && !canal.getOrigen().equals("")) {
            if (canal.getDestino() != null && !canal.getDestino().equals("")) {
                return getFilesConvert(getOrigen(date), ConverterApp.DIR_TMP, getDestino(date));
            } else {
                LOG.info("No hay directorio de destino registrado para el canal -> " + canal.getNombre());
            }
        } else {
            LOG.info("No hay directorio de origen registrado para el canal -> " + canal.getNombre());
        }
        return new ArrayList<>();
    }

    private String getOrigen(Date date) {
//        return canal.getOrigen() + df_anio.format(date) + DS + df_mes.format(date) + DS + df_dia.format(date) + DS;
        return canal.getOrigen() + df_anio.format(date) + df_mes.format(date) + df_dia.format(date) + DS;
    }

    private String getDestino(Date date) {
        String dir_destino = canal.getDestino() + canal.getAlias() + DS + df_anio.format(date) + df_mes.format(date) + df_dia.format(date) + DS;
        // Se verifica si existe la carpeta
        File carpeta_destino = new File(dir_destino);
        if (!carpeta_destino.exists()) {
            // Si no existe, se crea la carpeta de salida
            if (ISLINUX) {
//                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx"); // 777
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x"); // 755
                try {
                    Files.createDirectories(Paths.get(dir_destino), PosixFilePermissions.asFileAttribute(perms));
                    System.out.println(DF.format(new Date()) + "Se ha creado la carpeta de destino -> " + dir_destino);
                } catch (IOException e) {
                    LOG.warn("No se pudo crear la carpeta de destino -> " + dir_destino);
                    LOG.warn("Error getDestino:", e);
                }
            } else {
                if (carpeta_destino.mkdirs()) {
                    System.out.println(DF.format(new Date()) + "Se ha creado la carpeta de destino -> " + dir_destino);
                } else {
                    LOG.warn("No se pudo crear la carpeta de destino -> " + dir_destino);
                }
            }

        }
        return dir_destino;
    }

    private List<String[]> getFilesConvert(String dir_origen, String dir_destino_tmp, String dir_destino) {
        List<String[]> files_convert = new ArrayList<>();
        // Se valida si existe la ruta (carpeta) de origen
        File carpeta_origen = new File(dir_origen);
        if (carpeta_origen.exists() && carpeta_origen.isDirectory()) {
            if (carpeta_origen.listFiles().length > 0) {
                // La lista de archivos se ordena en orden inverso (descendente)
                String[] list_files = carpeta_origen.list();
                Comparator<String> comparador = Collections.reverseOrder();
                Arrays.sort(list_files, comparador);

                int pos = 0;
                for (String filein : list_files) {
                    if (filein.contains(".mkv") == true) {
                        // Directorio de origen (entrada -> archivo mkv)
                        String dir_file_in = dir_origen + filein;
                        File file_in = new File(dir_file_in);
                        if (file_in.exists() && file_in.isFile()) {
                            String[] data_file = filein.split("_");
                            String file_fecha = "";
                            String file_hora = "";
                            // Se obtiene la hora del archivo (del nombre del archivo)
                            if (data_file.length == 3) {
                                file_fecha = data_file[1];
                                file_hora = data_file[2];
                            }
                            // Se quitan los guiones medios de la hora y se cambia el formato del archivo de salida a .wmv
                            file_fecha = file_fecha.replace("-", "");
                            file_hora = file_hora.replace("-", "");
                            file_hora = file_hora.replace(".mkv", "." + config.getFto());
                            if (file_hora.length() == 10) {
                                // Directorio de salida (destino)
                                String fileout;
                                if (canal.getAlias() != null && !canal.getAlias().equals("")) {
                                    fileout = canal.getAlias() + "_" + file_fecha + "_" + file_hora;
                                } else {
                                    fileout = canal.getNombre() + "_" + file_fecha + "_" + file_hora;
                                }
                                // Dir. salida temporal 
                                String dir_file_out_tmp = dir_destino_tmp + fileout;
                                // Dir. salida final
                                String dir_file_out = dir_destino + fileout;

                                // Se obtiene el tamaño actual del archivo 
                                Long file_in_size = file_in.length();
                                // Se obtiene el registro de control para ver si existe
                                Control ctrl = getRegCtrl(filein);
                                Long old_size = new Long(0);
                                if (ctrl == null) {
                                    // Se inserta el registro de control
                                    insertRegCtrl(filein, file_in_size);
                                } else {
                                    old_size = ctrl.getSize();
                                }
                                // Se compara el tamaño del registro de control con el tamaño actual del archivo
                                if (file_in_size.equals(old_size)) {
                                    if (pos == 0) {
                                        String[] data_convert = {dir_file_in, dir_file_out_tmp, dir_file_out, filein, fileout, "true"};
                                        files_convert.add(data_convert);
                                    }
                                    // Verifica si no se ha movido el archivo temporal (convertir = false)
                                    File file_out_tmp = new File(dir_file_out_tmp);
                                    if (file_out_tmp.exists() && file_out_tmp.isFile()) {
                                        String[] data_convert = {dir_file_in, dir_file_out_tmp, dir_file_out, filein, fileout, "false"};
                                        files_convert.add(data_convert);
                                    }
                                } else {//Convertir!!!
                                    // Se actualiza el tamaño en el registro de control
                                    updateSizeCtrl(filein, file_in_size);
                                    // Arreglo de string con la info necesaria para convertir o mover el archivo
                                    String[] data_convert = {dir_file_in, dir_file_out_tmp, dir_file_out, filein, fileout, "true"};
                                    files_convert.add(data_convert);
                                }
                            } // FIN file_hora.length() == 10 (formato correcto de la hora y de la extensión del archivo "hhmmss.mp4")
                        } else {
                            LOG.warn("NO SE ENCONTRÓ EL ARCHIVO ORIGEN -> " + filein);
                        }
                    } // FIN if (filein.contains(".mkv") == true)
                    pos++;
                } // FIN for (String filein : list_files)
            } else {
                LOG.info("Canal " + canal.getNombre() + " > NO SE ENCONTRARON ARCHIVOS .MKV EN LA RUTA -> " + dir_origen);
            }
        } else {
            LOG.info("Canal " + canal.getNombre() + " > NO SE ENCONTRO LA RUTA DE ORIGEN -> " + dir_origen);
        }
        return files_convert;
    }

    private Control getRegCtrl(String filein) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Control> ctrl = session.createQuery("FROM Control WHERE file = :file", Control.class)
                    .setParameter("file", filein).getResultList();
            session.close();
            if (ctrl.isEmpty()) {
                return null;
            } else {
                return ctrl.get(0);
            }

        } catch (Exception e) {
            LOG.warn("Exception function getRegCtrl():" + e);
            return null;
        }
    }

    private void insertRegCtrl(String filein, Long size) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Control ctrl = new Control(filein, size);
            session.save(ctrl);

            transaction.commit();
            session.close();

        } catch (Exception e) {
            LOG.warn("Exception function insertRegCtrl():" + e);
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    private void updateSizeCtrl(String filein, Long size) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // Se obtiene el id, el tamaño y el  intento
            Control c = session.createQuery("FROM Control WHERE file = :file", Control.class)
                    .setParameter("file", filein)
                    .getSingleResult();

            Control ctrl = (Control) session.find(Control.class, c.getId());
            ctrl.setSize(size);

            transaction.commit();
            session.close();

        } catch (Exception e) {
            LOG.warn("Exception function updateSizeCtrl():" + e);
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    private void moverTemporal(Path path_out_tmp, Path path_out, String out) {
        Path path_final = null;
        int intento = 1;
        while (path_final == null && intento < config.getNint()) {
            try {
                //intenta mover el archivo temporal
                Files.move(path_out_tmp, path_out, StandardCopyOption.REPLACE_EXISTING);
//              path_final = Files.copy(path_out_tmp, path_out, options_copy);
                //Si llegas aqui, la operacion fue exitosa, sal del bucle
                path_final= path_out;
            } catch (IOException e) {
                LOG.warn("INTENTO " + intento + " PARA MOVER ARCHIVO -> " + out, e);
                try {
                    HiloCanal.sleep(config.getFint());
                } catch (InterruptedException ex) {
                    LOG.warn("Error al intentar mover el archivo -> " + out, ex);
                    Thread.currentThread().interrupt();
                }
            } 
            intento ++;
        }
        if (path_final != null) {
            if (ISLINUX) {
                //intenta asignar permisos al archivo en sistema Linux
                 try {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
                    Files.setPosixFilePermissions(path_final, perms);
                } catch (IOException e) {
                    LOG.error("Error al asignar permisos! -> " + path_final, e);
                }
            }
            System.out.println(DF.format(new Date()) + "ARCHIVO TEMPORAL MOVIDO CON ÉXITO A -> " + out);
        } else {
            LOG.error(DF.format(new Date()) + "NO SE PUDO MOVER EL ARCHIVO TEMPORAL A -> " + out);
            telegramNotifier.sendMessage("NO SE PUDO MOVER EL ARCHIVO TEMPORAL A ->" + out);
        }
    }   
}
