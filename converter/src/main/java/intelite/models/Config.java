package intelite.models;

import org.apache.commons.lang3.math.Fraction;

public class Config {

    private String fto;
    private Integer nint; // Número de intentos de pegado
    private Integer fint; // Frecuencia intento de pegado
    private String custom;
    private Integer res_w; // Resolución ancho (width)
    private Integer res_h; // Resolución alto (high)
    private Long vbr; // Bitrate video
    private Fraction fps; // Fotogramas por segundo
    private Integer asr; // Samplerate audio
    private Long abr; // Bitrate audio

    public Config() {
        super();
    }

    public Config(String fto, Integer nint, Integer fint, String custom, Integer res_w, Integer res_h, Long vbr, Fraction fps, Integer asr, Long abr) {
        this.fto = fto;
        this.nint = nint;
        this.fint = fint;
        this.custom = custom;
        this.res_w = res_w;
        this.res_h = res_h;
        this.vbr = vbr;
        this.fps = fps;
        this.asr = asr;
        this.abr = abr;
    }

    public String getFto() {
        return fto;
    }

    public void setFto(String fto) {
        this.fto = fto;
    }

    public Integer getNint() {
        return nint;
    }

    public void setNint(Integer nint) {
        this.nint = nint;
    }

    public Integer getFint() {
        return fint;
    }

    public void setFint(Integer fint) {
        this.fint = fint;
    }

    public Integer getRes_w() {
        return res_w;
    }

    public void setRes_w(Integer res_w) {
        this.res_w = res_w;
    }

    public Integer getRes_h() {
        return res_h;
    }

    public void setRes_h(Integer res_h) {
        this.res_h = res_h;
    }

    public Long getVbr() {
        return vbr;
    }

    public void setVbr(Long vbr) {
        this.vbr = vbr;
    }

    public Fraction getFps() {
        return fps;
    }

    public void setFps(Fraction fps) {
        this.fps = fps;
    }

    public Integer getAsr() {
        return asr;
    }

    public void setAsr(Integer asr) {
        this.asr = asr;
    }

    public Long getAbr() {
        return abr;
    }

    public void setAbr(Long abr) {
        this.abr = abr;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }
    
}
