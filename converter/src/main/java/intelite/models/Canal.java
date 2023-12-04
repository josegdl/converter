package intelite.models;

public class Canal {

    private String nombre;
    private String alias;
    private String origen;
    private String destino;
    private Integer delay;
    private Integer activo;

    public Canal() {
        super();
    }

    public Canal(String nombre) {
        super();
        this.nombre = nombre;
        this.activo = 1;
    }

    public Canal(String nombre, String alias, String origen, String destino, Integer activo, Integer delay) {
        super();
        this.nombre = nombre;
        this.alias = alias;
        this.origen = origen;
        this.destino = destino;
        this.activo = activo;
        this.delay = delay;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Canal{" + "nombre=" + nombre + ", alias=" + alias + ", origen=" + origen + ", destino=" + destino + ", activo=" + activo + ", delay=" + delay + '}';
    }

}
