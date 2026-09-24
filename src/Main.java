import java.io.*;
import java.util.Random;

public class Main {
    /**
     * Метод для создания файла со случайными данными указанного размера
     * @param filename Имя создаваемого файла
     * @param fileSize Размер файла в байтах
     * @param showProgress Показывать ли прогресс создания
     * @throws IOException Если произошла ошибка при создании файла
     */
    public static void createRandomFile(String filename, long fileSize, boolean showProgress) throws IOException {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("Размер файла должен быть больше 0");
        }

        System.out.println("Создание файла: " + filename);
        System.out.println("Размер: " + fileSize + " байт (" + formatSize(fileSize) + ")");

        long startTime = System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(filename);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            Random random = new Random();
            byte[] buffer = new byte[8192]; // 8 КБ буфер
            long written = 0;

            if (showProgress) {
                System.out.print("Прогресс: 0%");
            }

            while (written < fileSize) {
                // Заполняем буфер случайными байтами
                random.nextBytes(buffer);

                // Определяем сколько записать
                int toWrite = (int) Math.min(buffer.length, fileSize - written);

                // Записываем
                bos.write(buffer, 0, toWrite);
                written += toWrite;

                if (showProgress) {
                    // Обновляем прогресс каждые 5%
                    int progress = (int) ((written * 100) / fileSize);
                    if (progress % 5 == 0 || written == fileSize) {
                        System.out.print("\rПрогресс: " + progress + "%");
                    }
                }
            }

            if (showProgress) {
                System.out.println("\rПрогресс: 100%");
            }
        }

        long endTime = System.currentTimeMillis();
        double timeSec = (endTime - startTime) / 1000.0;

        File file = new File(filename);
        System.out.println("\nФайл создан: " + file.getAbsolutePath());
        System.out.println("Фактический размер: " + file.length() + " байт");
        System.out.printf("Время создания: %.3f сек\n", timeSec);

        if (timeSec > 0) {
            double speed = file.length() / (timeSec * 1024 * 1024);
            System.out.printf("Скорость: %.2f МБ/сек\n", speed);
        }
    }

    /**
     * Метод для создания файла с определенным содержимым
     * @param filename Имя создаваемого файла
     * @param content Содержимое файла в виде строки
     * @throws IOException Если произошла ошибка при создании файла
     */
    public static void createTextFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
        System.out.println("Создан текстовый файл: " + filename);
        System.out.println("Размер: " + content.length() + " байт");
        System.out.println("Содержимое: \"" + content + "\"");
    }

    /**
     * Метод для создания файла с повторяющимся паттерном
     * @param filename Имя создаваемого файла
     * @param fileSize Размер файла в байтах
     * @param pattern Паттерн для повторения
     * @throws IOException Если произошла ошибка при создании файла
     */
    public static void createPatternFile(String filename, long fileSize, byte[] pattern) throws IOException {
        if (pattern == null || pattern.length == 0) {
            throw new IllegalArgumentException("Паттерн не может быть пустым");
        }

        try (FileOutputStream fos = new FileOutputStream(filename);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            long written = 0;

            while (written < fileSize) {
                int toWrite = (int) Math.min(pattern.length, fileSize - written);
                bos.write(pattern, 0, toWrite);
                written += toWrite;
            }
        }

        File file = new File(filename);
        System.out.println("Создан файл с паттерном: " + filename);
        System.out.println("Размер: " + file.length() + " байт");
        System.out.println("Паттерн: " + bytesToHex(pattern));
    }

    /**
     * Вспомогательный метод для форматирования размера
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Б";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f КБ", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f МБ", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f ГБ", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Вспомогательный метод для преобразования байтов в HEX строку
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b & 0xFF));
        }
        return hex.toString().trim();
    }

    public static void main(String[] args) {
        try {
            createRandomFile("test300Mb.txt", 300 * 1024 * 1024, true);
        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}