package me.lssupportteam.ipdynamic.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IPUtils {

    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern IP_WILDCARD_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|\\*)\\.)+" +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|\\*)$"
    );

    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        return IP_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidIpPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        return IP_WILDCARD_PATTERN.matcher(pattern).matches();
    }

    public static boolean matches(String ip, String pattern) {
        if (!isValidIpAddress(ip)) return false;
        if (!isValidIpPattern(pattern)) return false;

        String[] ipParts = ip.split("\\.");
        String[] patternParts = pattern.split("\\.");

        if (ipParts.length != 4 || patternParts.length != 4) return false;

        for (int i = 0; i < 4; i++) {
            if (!patternParts[i].equals("*") && !patternParts[i].equals(ipParts[i])) {
                return false;
            }
        }

        return true;
    }

    public static List<String> generateIpRange(String pattern) {
        List<String> ips = new ArrayList<>();

        if (!isValidIpPattern(pattern)) {
            return ips;
        }

        String[] parts = pattern.split("\\.");
        if (parts.length != 4) return ips;


        int wildcardCount = 0;
        for (String part : parts) {
            if (part.equals("*")) wildcardCount++;
        }


        if (wildcardCount > 1) {
            return ips; // No generar lista para OP2+ debido al tamaño
        }

        generateIpRangeRecursive(parts, 0, new String[4], ips);
        return ips;
    }

    private static void generateIpRangeRecursive(String[] pattern, int index, String[] current, List<String> result) {
        if (index == 4) {
            result.add(String.join(".", current));
            return;
        }

        if (pattern[index].equals("*")) {
            for (int i = 0; i <= 255; i++) {
                current[index] = String.valueOf(i);
                generateIpRangeRecursive(pattern, index + 1, current, result);
            }
        } else {
            current[index] = pattern[index];
            generateIpRangeRecursive(pattern, index + 1, current, result);
        }
    }

    public static int countAffectedIps(String pattern) {
        if (!isValidIpPattern(pattern)) return 0;

        String[] parts = pattern.split("\\.");
        int count = 1;

        for (String part : parts) {
            if (part.equals("*")) {
                count *= 256;
            }
        }

        return count;
    }

    public static String getIpFromPattern(String pattern, int index) {
        List<String> ips = generateIpRange(pattern);
        if (index >= 0 && index < ips.size()) {
            return ips.get(index);
        }
        return null;
    }

    public static boolean isLocalIp(String ip) {
        if (!isValidIpAddress(ip)) return false;

        return ip.startsWith("127.") ||
               ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               (ip.startsWith("172.") && isPrivate172(ip));
    }

    private static boolean isPrivate172(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String resolveHostname(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static String getHostname(String ip) {
        if (!isValidIpAddress(ip)) return null;

        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static String maskIp(String ip, int level) {
        if (!isValidIpAddress(ip)) return ip;

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return ip;

        switch (level) {
            case 1: // Ocultar último octeto
                return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
            case 2: // Ocultar últimos 2 octetos
                return parts[0] + "." + parts[1] + ".*.*";
            case 3: // Ocultar últimos 3 octetos
                return parts[0] + ".*.*.*";
            default:
                return ip;
        }
    }

    public static boolean areInSameRange(String ip1, String ip2, int maskLevel) {
        if (!isValidIpAddress(ip1) || !isValidIpAddress(ip2)) return false;

        String masked1 = maskIp(ip1, maskLevel);
        String masked2 = maskIp(ip2, maskLevel);

        return masked1.equals(masked2);
    }

    public static String incrementIp(String ip) {
        if (!isValidIpAddress(ip)) return null;

        String[] parts = ip.split("\\.");
        int[] octets = new int[4];

        for (int i = 0; i < 4; i++) {
            octets[i] = Integer.parseInt(parts[i]);
        }


        for (int i = 3; i >= 0; i--) {
            if (octets[i] < 255) {
                octets[i]++;
                break;
            } else {
                octets[i] = 0;
                if (i == 0) {
                    return null; // Overflow
                }
            }
        }

        return String.format("%d.%d.%d.%d", octets[0], octets[1], octets[2], octets[3]);
    }
}