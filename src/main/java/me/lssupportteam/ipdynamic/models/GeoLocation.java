package me.lssupportteam.ipdynamic.models;

public class GeoLocation {

    private String country;
    private String countryCode;
    private String region;
    private String regionName;
    private String city;
    private String zip;
    private double latitude;
    private double longitude;
    private String timezone;
    private String isp;
    private String org;
    private String as;
    private String query;
    private boolean proxy;
    private boolean hosting;
    private boolean mobile;

    public GeoLocation() {

    }

    public GeoLocation(String country, String countryCode, String city, String region) {
        this.country = country;
        this.countryCode = countryCode;
        this.city = city;
        this.region = region;
    }

    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();

        if (city != null && !city.isEmpty()) {
            sb.append(city);
        }

        if (regionName != null && !regionName.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(regionName);
        }

        if (country != null && !country.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }

        if (sb.length() == 0) {
            return "Ubicación desconocida";
        }

        return sb.toString();
    }

    public String getShortLocation() {
        if (city != null && !city.isEmpty() && countryCode != null) {
            return city + ", " + countryCode;
        } else if (country != null && !country.isEmpty()) {
            return country;
        }
        return "Desconocido";
    }

    public boolean isRisky() {
        return proxy || hosting;
    }


    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getIsp() { return isp; }
    public void setIsp(String isp) { this.isp = isp; }

    public String getOrg() { return org; }
    public void setOrg(String org) { this.org = org; }

    public String getAs() { return as; }
    public void setAs(String as) { this.as = as; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public boolean isProxy() { return proxy; }
    public void setProxy(boolean proxy) { this.proxy = proxy; }

    public boolean isHosting() { return hosting; }
    public void setHosting(boolean hosting) { this.hosting = hosting; }

    public boolean isMobile() { return mobile; }
    public void setMobile(boolean mobile) { this.mobile = mobile; }
}