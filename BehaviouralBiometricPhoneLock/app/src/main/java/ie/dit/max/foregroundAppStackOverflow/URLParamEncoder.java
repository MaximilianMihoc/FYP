package ie.dit.max.foregroundAppStackOverflow;

/**
 * Class used to encode parameters that are sent in the URL when the user search for questions in Stack Overflow
 * This was mostly used to convert the Search terms that the user inputs so that it will not break the url format
 * Converts special characters into hex values to be used in the URL.
 * The characters converted are believed to be Unsafe for the URL.
 *
 * Reference: http://stackoverflow.com/questions/724043/http-url-address-encoding-in-java/4605816#4605816
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class URLParamEncoder
{
    /**
     * Method to check the url for unsafe characters and convert them to safe characters.
     *
     * @param input String unsafeUrl
     * @return String safeUrl
     */
    public static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    /**
     * Convert characters to hex
     * @param ch int
     * @return char
     */
    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    /**
     * Check if a character is safe or Unsafe
     *
     * @param ch char
     * @return boolean
     */
    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }
    /* Reference End. */
}
