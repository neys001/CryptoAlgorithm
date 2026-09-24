import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.util.Scanner;

public class DESOFB {

    private static final String ALGORITHM = "DES";
    private static final String TRANSFORMATION = "DES/OFB/PKCS5Padding";
    private static final int DES_KEY_SIZE = 8;
    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB для лучшей производительности

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n DES-OFB Шифрователь ");
            System.out.println("1. Зашифровать файл");
            System.out.println("2. Расшифровать файл");
            System.out.println("3. Выход");
            System.out.print("Выберите действие: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите число от 1 до 3");
                continue;
            }

            switch (choice) {
                case 1:
                    encryptFile(scanner);
                    break;
                case 2:
                    decryptFile(scanner);
                    break;
                case 3:
                    System.out.println("Выход...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Неверный выбор!");
            }
        }
    }

    private static void encryptFile(Scanner scanner) {
        System.out.println("\n Шифрование файла: ");

        System.out.print("Введите путь к исходному файлу: ");
        String inputPath = scanner.nextLine().trim();

        if (inputPath.isEmpty()) {
            System.out.println("Ошибка: путь к файлу не может быть пустым!");
            return;
        }

        System.out.print("Введите ключ (8 символов): ");
        String key = scanner.nextLine().trim();

        System.out.print("Введите IV (8 символов): ");
        String iv = scanner.nextLine().trim();

        // Проверка длины
        if (key.length() != DES_KEY_SIZE || iv.length() != DES_KEY_SIZE) {
            System.out.println("Ошибка: ключ и IV должны быть 8 символов!");
            return;
        }

        // Автоматическое имя для зашифрованного файла
        String outputPath = inputPath + ".encrypted";

        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("Ошибка: файл не найден!");
                return;
            }

            System.out.println("\nНачало шифрования...");
            long startTime = System.currentTimeMillis();

            // Показываем информацию о файле
            System.out.println("\n_Информация о файле_");
            System.out.println("Исходный файл: " + inputFile.getName());
            System.out.println("Размер: " + inputFile.length() + " байт (" +
                    String.format("%.2f", inputFile.length() / 1073741824.0) + " ГБ)");
            System.out.println("Ключ: \"" + key + "\"");
            System.out.println("IV: \"" + iv + "\"");

            // Подготовка ключа и IV
            byte[] keyBytes = key.getBytes("ISO-8859-1");
            byte[] ivBytes = iv.getBytes("ISO-8859-1");

            System.out.println("IV в HEX: " + bytesToHex(ivBytes));

            // Читаем и анализируем первый байт БЕЗ загрузки всего файла
            byte firstByte = 0;
            try (FileInputStream fisForFirstByte = new FileInputStream(inputFile)) {
                int firstByteInt = fisForFirstByte.read();
                if (firstByteInt != -1) {
                    firstByte = (byte) firstByteInt;
                    System.out.println("\nАнализ первого символа");
                    System.out.println("Символ: '" + (char)firstByte + "'");
                    System.out.println("ASCII: " + (firstByte & 0xFF));
                    System.out.println("HEX: 0x" + String.format("%02X", firstByte & 0xFF));
                    System.out.println("Двоичное: " + toBinaryString(firstByte));
                }
            }

            // Подготовка шифрования
            DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Потоковое шифрование
            long totalBytes = 0;
            try (FileInputStream fis = new FileInputStream(inputFile);
                 FileOutputStream fos = new FileOutputStream(outputPath)) {

                // Записываем IV в начало зашифрованного файла
                fos.write(ivBytes);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long lastProgressUpdate = System.currentTimeMillis();

                // Прогресс-бар для больших файлов
                long fileSize = inputFile.length();
                int lastPercent = -1;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] encrypted = cipher.update(buffer, 0, bytesRead);
                    if (encrypted != null) {
                        fos.write(encrypted);
                    }
                    totalBytes += bytesRead;

                    // Обновление прогресса каждые 100 МБ или 5%
                    long currentTime = System.currentTimeMillis();
                    if (fileSize > 100 * 1024 * 1024 &&
                            (currentTime - lastProgressUpdate > 5000 || bytesRead == -1)) {

                        int percent = (int) ((totalBytes * 100) / fileSize);
                        if (percent != lastPercent) {
                            System.out.printf("Прогресс: %d%% (%.2f/%.2f ГБ)\n",
                                    percent,
                                    totalBytes / 1073741824.0,
                                    fileSize / 1073741824.0);
                            lastPercent = percent;
                        }
                        lastProgressUpdate = currentTime;
                    }
                }

                byte[] finalBytes = cipher.doFinal();
                if (finalBytes != null) {
                    fos.write(finalBytes);
                }
            }

            long endTime = System.currentTimeMillis();
            double timeSec = (endTime - startTime) / 1000.0;

            // Результаты
            System.out.println("\n_Результат шифрования_");
            System.out.println("Зашифрованный файл: " + new File(outputPath).getName());
            System.out.printf("Время выполнения: %.3f сек\n", timeSec);

            File outputFile = new File(outputPath);
            long originalSize = inputFile.length();
            long encryptedSize = outputFile.length();
            System.out.println("Исходный размер: " + originalSize + " байт");
            System.out.println("Зашифрованный размер: " + encryptedSize + " байт");

            if (originalSize > 0) {
                double sizeRatio = (double) encryptedSize / originalSize;
                System.out.printf("Коэффициент увеличения: %.2f\n", sizeRatio);
                System.out.println("(IV добавляет 8 байт + padding до 8 байт)");
            }

            if (timeSec > 0) {
                double speed = inputFile.length() / (timeSec * 1024 * 1024);
                System.out.printf("Скорость шифрования: %.2f МБ/сек\n", speed);
            }

            // Показываем начало зашифрованного файла
            System.out.println("\nНачало зашифрованного файла");
            showFirstBytes(outputPath, 32);

        } catch (Exception e) {
            System.out.println("Ошибка при шифровании: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void decryptFile(Scanner scanner) {
        System.out.println("\n=== ДЕШИФРОВАНИЕ ФАЙЛА ===");

        System.out.print("Введите путь к зашифрованному файлу: ");
        String inputPath = scanner.nextLine().trim();

        if (inputPath.isEmpty()) {
            System.out.println("Ошибка: путь к файлу не может быть пустым!");
            return;
        }

        System.out.print("Введите ключ (8 символов): ");
        String key = scanner.nextLine().trim();

        if (key.length() != DES_KEY_SIZE) {
            System.out.println("Ошибка: ключ должен быть 8 символов!");
            return;
        }

        String outputPath;
        if (inputPath.endsWith(".encrypted")) {
            outputPath = inputPath.substring(0, inputPath.length() - 10) + ".decrypted";
        } else {
            outputPath = inputPath + ".decrypted";
        }

        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("Ошибка: файл не найден!");
                return;
            }

            System.out.println("\nНачало дешифрования...");

            // Читаем весь файл
            byte[] fileData;
            try (FileInputStream fis = new FileInputStream(inputFile)) {
                fileData = fis.readAllBytes();
            }

            System.out.println("Размер файла: " + fileData.length + " байт");

            // Проверяем минимальный размер
            if (fileData.length < 8) {
                System.out.println("❌ Файл слишком мал (меньше 8 байт)!");
                return;
            }

            // Извлекаем IV (первые 8 байт)
            byte[] ivBytes = new byte[8];
            System.arraycopy(fileData, 0, ivBytes, 0, 8);

            // Зашифрованные данные (все что после IV)
            int encryptedDataLength = fileData.length - 8;
            byte[] encryptedData = new byte[encryptedDataLength];
            System.arraycopy(fileData, 8, encryptedData, 0, encryptedDataLength);

            System.out.println("Размер зашифрованных данных: " + encryptedDataLength + " байт");

            // === АВТОМАТИЧЕСКИЙ PADDING ЕСЛИ НУЖНО ===
            if (encryptedDataLength % 8 != 0) {
                System.out.println("⚠️  Внимание: размер данных не кратен 8!");
                System.out.println("   Добавляем автоматический padding...");

                // Вычисляем сколько байт нужно добавить
                int neededBytes = 8 - (encryptedDataLength % 8);

                // Создаем новый массив с правильным размером
                byte[] paddedData = new byte[encryptedDataLength + neededBytes];
                System.arraycopy(encryptedData, 0, paddedData, 0, encryptedDataLength);

                // Заполняем добавленные байты нулями
                for (int i = encryptedDataLength; i < paddedData.length; i++) {
                    paddedData[i] = 0;
                }

                System.out.println("   Добавлено " + neededBytes + " нулевых байт");
                System.out.println("   Новый размер: " + paddedData.length + " байт");

                // Заменяем данные на дополненные
                encryptedData = paddedData;
            }

            // Подготовка ключа
            byte[] keyBytes = key.getBytes("ISO-8859-1");

            DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Дешифруем
            byte[] decryptedData = cipher.doFinal(encryptedData);

            // Записываем результат
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(decryptedData);
            }

            System.out.println("\n✅ Дешифрование успешно!");
            System.out.println("Файл сохранен: " + outputPath);
            System.out.println("Размер расшифрованного файла: " + decryptedData.length + " байт");

        } catch (Exception e) {
            System.out.println("❌ Ошибка при дешифровании: " + e.getMessage());
            System.out.println("\nВозможные причины:");
            System.out.println("1. Неправильный ключ");
            System.out.println("2. Файл поврежден");
            System.out.println("3. Неверный формат файла");
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private static String readFirstLines(String filename, int maxLines) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < maxLines) {
                if (lineCount > 0) result.append("\\n");
                result.append(line);
                lineCount++;
            }
        }
        return result.toString();
    }

    // Обновленный метод сравнения файлов для работы с большими файлами
    private static boolean compareFilesStreaming(String file1, String file2) throws IOException {
        try (InputStream in1 = new FileInputStream(file1);
             InputStream in2 = new FileInputStream(file2)) {

            byte[] buffer1 = new byte[BUFFER_SIZE];
            byte[] buffer2 = new byte[BUFFER_SIZE];

            long bytesCompared = 0;
            long totalBytes = new File(file1).length();
            int lastPercent = -1;

            while (true) {
                int bytes1 = in1.read(buffer1);
                int bytes2 = in2.read(buffer2);

                if (bytes1 == -1 && bytes2 == -1) return true;
                if (bytes1 != bytes2) return false;

                for (int i = 0; i < bytes1; i++) {
                    if (buffer1[i] != buffer2[i]) return false;
                }

                bytesCompared += bytes1;

                // Прогресс для больших файлов
                if (totalBytes > 100 * 1024 * 1024) {
                    int percent = (int) ((bytesCompared * 100) / totalBytes);
                    if (percent != lastPercent && percent % 10 == 0) {
                        System.out.printf("Сравнение файлов: %d%%\n", percent);
                        lastPercent = percent;
                    }
                }
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    private static String toBinaryString(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    private static void showFirstBytes(String filename, int maxToShow) throws IOException {
        File file = new File(filename);
        long fileSize = file.length();

        // Сколько байт реально показывать (не больше размера файла)
        int bytesToShow = (int) Math.min(fileSize, maxToShow);

        if (bytesToShow <= 0) {
            System.out.println("Файл пуст!");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[bytesToShow];
            int bytesRead = fis.read(buffer);

            if (bytesRead <= 0) {
                System.out.println("Не удалось прочитать байты из файла");
                return;
            }

            System.out.println("Показано " + bytesRead + " байт из " + fileSize + " (запрошено: " + maxToShow + "):");

            // HEX вывод
            System.out.print("HEX: ");
            for (int i = 0; i < bytesRead; i++) {
                System.out.print(String.format("%02X ", buffer[i] & 0xFF));
                if (i == 7 && bytesRead > 8) {
                    System.out.print("| ");
                }
            }

            // Если файл меньше запрошенного, покажем это
            if (bytesRead < maxToShow) {
                System.out.print(" <конец файла>");
            }
            System.out.println();

            // DEC вывод
            System.out.print("DEC: ");
            for (int i = 0; i < bytesRead; i++) {
                System.out.print(String.format("%3d ", buffer[i] & 0xFF));
                if (i == 7 && bytesRead > 8) {
                    System.out.print("| ");
                }
            }

            if (bytesRead < maxToShow) {
                System.out.print(" <конец файла>");
            }
            System.out.println();

            // Разметка для зашифрованных файлов
            if (bytesRead >= 8) {
                System.out.println("IV (8 байт), потом зашифрованные данные");

                // Для очень маленьких файлов покажем что где
                if (bytesRead == 16) {
                    System.out.println("\nСтруктура файла (16 байт):");
                    System.out.println("1-8 байт: IV = \"" + new String(buffer, 0, 8, "ISO-8859-1") + "\"");
                    System.out.println("9-16 байт: Зашифрованный блок (8 байт)");

                    // Если это был маленький файл, покажем padding информацию
                    if (fileSize == 16) {
                        System.out.println("\nПримечание: 1 исходный байт → 8 байт после padding");
                        System.out.println("Первый зашифрованный байт (9-й): 0x" +
                                String.format("%02X", buffer[8] & 0xFF) +
                                " = ваш зашифрованный символ");
                    }
                }
            }
        }
    }
}