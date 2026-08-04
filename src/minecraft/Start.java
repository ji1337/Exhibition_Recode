import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Start
{
    private static final String[] DEFAULT_ARGUMENTS = new String[] {
            "--version", "mcp",
            "--accessToken", "0",
            "--assetsDir", "assets",
            "--assetIndex", "1.8",
            "--userProperties", "{}"
    };

    public static void main(String... args) throws Exception
    {
        Class.forName("Main").getDeclaredMethod("bruh", String[].class).invoke(null, new Object[] {addMissingDefaults(DEFAULT_ARGUMENTS, args)});
    }

    static String[] addMissingDefaults(String[] defaults, String[] arguments)
    {
        List<String> missingDefaults = new ArrayList<>();

        for (int index = 0; index < defaults.length; index += 2)
        {
            String option = defaults[index];

            if (!hasOption(arguments, option))
            {
                missingDefaults.add(option);
                missingDefaults.add(defaults[index + 1]);
            }
        }

        return concat(missingDefaults.toArray(new String[missingDefaults.size()]), arguments);
    }

    private static boolean hasOption(String[] arguments, String option)
    {
        for (String argument : arguments)
        {
            if (option.equals(argument) || argument.startsWith(option + "="))
            {
                return true;
            }
        }

        return false;
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
