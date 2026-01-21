package com.policia.batch.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@XmlRootElement(name = "policia-data")
public class PoliciaData {

    @NotBlank(message = "ID no puede estar vacío")
    private String id;
    
    @NotBlank(message = "Nombre no puede estar vacío")
    private String nombre;
    
    @NotNull(message = "Rango no puede ser nulo")
    private String rango;
    
    private String unidad;
    private String estado;

    // Constructor por defecto requerido por JAXB
    public PoliciaData() {}

    public PoliciaData(String id, String nombre, String rango, String unidad, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.rango = rango;
        this.unidad = unidad;
        this.estado = estado;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @XmlElement
    public String getRango() {
        return rango;
    }

    public void setRango(String rango) {
        this.rango = rango;
    }

    @XmlElement
    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    @XmlElement
    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "PoliciaData{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", rango='" + rango + '\'' +
                ", unidad='" + unidad + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
