package com.finflow.receipt;

import com.finflow.exception.ProviderNotConfiguredException;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "finflow.receipt.provider", havingValue = "TESSERACT")
public class TesseractReceiptScanProvider implements ReceiptScanProvider {

    private static final Logger log = LoggerFactory.getLogger(TesseractReceiptScanProvider.class);

    private final String dataPath;
    private final String language;

    public TesseractReceiptScanProvider(@Value("${finflow.receipt.tesseract.data-path:}") String dataPath,
                                        @Value("${finflow.receipt.tesseract.language:eng}") String language,
                                        @Value("${finflow.receipt.tesseract.library-path:}") String libraryPath) {
        this.dataPath = dataPath;
        this.language = language;
        if (libraryPath != null && !libraryPath.isBlank()) {
            String existing = System.getProperty("jna.library.path", "");
            String merged = existing.isBlank() ? libraryPath.trim() : existing + File.pathSeparator + libraryPath.trim();
            System.setProperty("jna.library.path", merged);
        }
    }

    @Override
    public String name() {
        return "TESSERACT";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public OcrResult extract(byte[] image) {
        BufferedImage buffered = readImage(image);
        Tesseract tesseract = new Tesseract();
        if (dataPath != null && !dataPath.isBlank()) {
            tesseract.setDatapath(dataPath.trim());
        }
        tesseract.setLanguage(language);
        try {
            String text = tesseract.doOCR(buffered);
            List<Word> words = tesseract.getWords(buffered, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            List<OcrWord> ocrWords = new ArrayList<>();
            double sum = 0;
            for (Word word : words) {
                if (word.getText() == null || word.getText().isBlank()) continue;
                ocrWords.add(new OcrWord(word.getText().trim(), word.getConfidence()));
                sum += word.getConfidence();
            }
            double mean = ocrWords.isEmpty() ? 0 : sum / ocrWords.size();
            return new OcrResult(text == null ? "" : text, ocrWords, mean);
        } catch (Throwable throwable) {
            log.warn("Receipt OCR failed: {}", throwable.toString());
            throw new ProviderNotConfiguredException("Receipt OCR engine could not process the image. Confirm the tesseract native library and language data are installed.");
        }
    }

    private BufferedImage readImage(byte[] image) {
        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(image));
            if (buffered == null) {
                throw new ProviderNotConfiguredException("The uploaded file could not be read as an image.");
            }
            return buffered;
        } catch (ProviderNotConfiguredException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProviderNotConfiguredException("The uploaded file could not be read as an image.");
        }
    }
}
