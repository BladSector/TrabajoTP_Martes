package ar.edu.cafe.fiscal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cafe.fiscal")
public class ConfiguracionFiscal {
    private String entorno = "SIMULACION";
    private String razonSocial = "Cafe Positivo";
    private String cuit = "";
    private String domicilio = "";
    private String condicionIva = "IVA EXENTO";
    private String tipoFactura = "C";
    private int puntoVenta = 8;
    private double alicuotaIva = 0.0;

    public String getEntorno() { return entorno; }
    public void setEntorno(String entorno) { this.entorno = entorno; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getCondicionIva() { return condicionIva; }
    public void setCondicionIva(String condicionIva) { this.condicionIva = condicionIva; }

    public String getTipoFactura() { return tipoFactura; }
    public void setTipoFactura(String tipoFactura) { this.tipoFactura = tipoFactura; }

    public int getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(int puntoVenta) { this.puntoVenta = puntoVenta; }

    public double getAlicuotaIva() { return alicuotaIva; }
    public void setAlicuotaIva(double alicuotaIva) { this.alicuotaIva = alicuotaIva; }
}
