package io.reign.lease;

import io.reign.PathScheme;
import io.reign.PathType;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LeaseUtil {

    private static final Comparator<String> RESERVATION_COMPARATOR = new LeaseReservationComparator("_");

    public static String leasePoolPath(PathScheme pathScheme, String clusterId, String poolId, int poolSize) {
        return pathScheme.getAbsolutePath(PathType.LEASE, clusterId,
                poolId, Integer.toString(poolSize));
    }

    public static String leasePath(PathScheme pathScheme, String clusterId, String poolId, int poolSize, String leaseId) {
        return pathScheme.getAbsolutePath(PathType.LEASE, clusterId,
                poolId, Integer.toString(poolSize), leaseId);
    }

    public static byte[] reservationData(String holderId, long durationMillis) throws UnsupportedEncodingException {
        holderId = holderId.replace('"', '\'');
        return ("{\"holderId\":\"" + holderId + "\",\"durationMillis\":\"'" + durationMillis + "','lastModified':'"
                + System.currentTimeMillis() + "'\"}")
                .getBytes("UTF-8");
    }

    public static String reservationPathPrefix(PathScheme pathScheme, String poolPath, long durationMillis) {
        return pathScheme.joinPaths(poolPath, "L_" + durationMillis + "_");
    }

    public static long durationMillisFromLeaseId(String leaseId) {
        String durationMillisString = leaseId.substring(leaseId.indexOf('_') + 1, leaseId.lastIndexOf('_'));
        return Long.parseLong(durationMillisString);
    }

    public static List<String> sortReservationList(List<String> reservationList) {
        Collections.sort(reservationList, RESERVATION_COMPARATOR);
        return reservationList;
    }

    public static class LeaseReservationComparator implements Comparator<String> {

        private final String delimiter;

        public LeaseReservationComparator(String delimiter) {
            super();
            this.delimiter = delimiter;
        }

        @Override
        public int compare(String arg0, String arg1) {
            if (arg0 == null && arg1 != null) {
                return -1;
            }
            if (arg0 != null && arg1 == null) {
                return 1;
            }

            long seq0 = parseSequenceNumber(arg0, delimiter);
            long seq1 = parseSequenceNumber(arg1, delimiter);

            if (seq0 > seq1) {
                return 1;
            }
            if (seq0 < seq1) {
                return -1;
            }

            return 0;
        }

        long parseSequenceNumber(String nodePath, String delimiter) {
            try {
                int delimiterIndex = nodePath.lastIndexOf(delimiter);
                return Long.parseLong(nodePath.substring(delimiterIndex + 1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Could parse sequence number:  " + e, e);
            }
        }
    }

}
