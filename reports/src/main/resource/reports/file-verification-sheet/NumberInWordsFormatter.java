import net.sf.jasperreports.engine.JRDefaultScriptlet;

/**
 * Скриплет для прописи чисел
 */
public class NumberInWordsFormatter extends JRDefaultScriptlet {

    final String[] units = {"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
    final String[] unitsFor1000 = {"", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"};
    final String[] tensLt20 = {"десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать",
            "пятнадцать", "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"};
    final String[] tensGt20 = {"", "", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят", "восемьдесят", "девяносто"};
    final String[] hundreds = {"", "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот", "восемьсот", "девятьсот"};

    /**
     * Получение числа прописью
     * @param number - число
     */
    public String getNumberInWords(Integer number) {
        if (number == null) {
            return "";
        }
        
        if (number == 0) {
            return " (ноль)";
        }

        String inWords = "";

        if (number < 1000000000) { // думаю, будет достаточно прописывать числа до миллиарда (не включительно)
            // N миллионов
            int curLt1000 = number / 1000000;
            if (number >= 1000000) {
                inWords += getLt1000(curLt1000, false);
                inWords += getPlaceNameDeclension(curLt1000, false);
            }
            // N тысяч
            curLt1000 = number % 1000000 / 1000;
            if (number % 1000000 >= 1000) {
                inWords += " " + getLt1000(curLt1000, true);
                inWords += getPlaceNameDeclension(curLt1000, true);
            }
            // N
            curLt1000 = number % 1000;
            inWords += " " + getLt1000(curLt1000, false);
        }

        inWords = inWords.trim().replaceAll(" +", " ");
        return inWords.isEmpty() ? "" : " (" + inWords + ")";
    }

    /**
     * Получение числа прописью до 1000
     * @param num - число до 1000 (включительно)
     * @param is1000 - для правильного окончания (#: двА миллиона / двЕ тысячи)
     */
    private String getLt1000(int num, boolean is1000) {
        return hundreds[num / 100] + " " + (
                num % 100 / 10 == 1 ? tensLt20[num % 10]
                        : (tensGt20[num % 100 / 10] + " " + (is1000 ? unitsFor1000[num % 10] : units[num % 10])));
    }

    /**
     * Склонение слов "тысяча" и "миллион"
     * @param num - число до 1000 (включительно)
     * @param is1000 - склонение слова "миллион"
     */
    private String getPlaceNameDeclension(int num, boolean is1000) {
        if (num % 10 == 0
                || num % 10 > 4
                || num % 100 / 10 == 1) {
            return is1000 ? " тысяч" : " миллионов";
        } else if (num % 10 != 1) {
            return is1000 ? " тысячи" : " миллиона";
        } else {
            return is1000 ? " тысяча" : " миллион";
        }
    }
}