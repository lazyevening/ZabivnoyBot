import org.glassfish.jersey.jaxb.internal.XmlCollectionJaxbProvider;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot implements ISender{
    private final RequestHandler requestHandler = new RequestHandler();
    Map<Long, User> Users = new HashMap<>();
    @Override
    public void onUpdateReceived(Update update) {
        var answer = new SendMessage();
        if (update.hasMessage()){
            answer = handleMessage(update);
            }
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        /*long chat_id = update.getMessage().getChatId();

        if (!Users.containsKey(chat_id)){
            Users.put(chat_id, new Session(1));
        }
        int cond = Users.get(chat_id).state;


            switch (cond) {
                case 0:
                    sendHelp(chat_id);
                    Users.get(chat_id).state = 1;
                    break;
                case 1:
                    if (update.getMessage().hasText()) {
                        String message_text = update.getMessage().getText();
                        switch (message_text) {
                            case Constants.ADD_TEXT_COMMAND:
                                Users.get(chat_id).state = 2;
                                sendMessage(chat_id, Constants.GET_IMAGE_MSG);
                                break;
                            case Constants.START_COMMAND:
                                sendHelp(chat_id);
                                break;
                            case Constants.CREATORS_COMMAND:
                                sendMessage(chat_id, Constants.CREATORS_MSG);
                                break;
                            default:
                                sendMessage(chat_id, Constants.ERROR_MSG);
                                break;
                        }
                    }
                    break;
                case 2:
                    if (update.getMessage().hasPhoto()) {
                        setPhoto(chat_id, update);
                        Users.get(chat_id).state = 3;
                        sendMessage(chat_id, Constants.GET_TEXT_MSG);
                    } else {
                        sendMessage(chat_id, Constants.GET_IMAGE_MSG);
                    }
                    break;
                case 3:
                    if (update.getMessage().hasText()) {
                        String text = update.getMessage().getText();
                        sendPhotoWithText(chat_id, text);
                        Users.get(chat_id).state = 1;
                        sendHelp(chat_id);
                    } else sendMessage(chat_id, Constants.GET_TEXT_MSG);
                    break;
                default:
                    break;
            }*/
        }
    private SendMessage handleMessage(Update update){
        String message = update.getMessage().getText();
        var answer = new SendMessage();
        answer.setChatId(update.getMessage().getChatId());
        String uid = update.getMessage().getFrom().getId().toString();
        String result = requestHandler.handle(uid, update.getMessage().getChatId().toString(), message, this);
        answer.setText(result);
        return answer;
    }

    private File downloadPhoto(String filePath) {
        try {
            return downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.out.println("download-error");
        }
        return null;
    }

    private void sendHelp(long chat_id){
        sendMessage(chat_id, Constants.HELP_MSG);
    }

    public void sendMessage(long chat_id, String message_text){
        SendMessage message = new SendMessage()
                .setChatId(chat_id)
                .setText(message_text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendPhotoWithText(long chat_id, String text){
        BufferedImage image = Users.get(chat_id).image;
        try {
            ImageProcessor.textToImage(image, text);
            ImageIO.write(image, "jpg", new File(Users.get(chat_id).file.toString()));
        } catch (IOException e){
            e.printStackTrace();
        }

        SendPhoto message = (new SendPhoto()).setChatId(chat_id).setPhoto(Users.get(chat_id).file);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void setPhoto(long chat_id, Update update){
        List<PhotoSize> photos = update.getMessage().getPhoto();

        String f_id = Objects.requireNonNull(photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null)).getFileId();

        GetFile getFile = new GetFile().setFileId(f_id);

        String filePath = null;
        try {
            filePath = execute(getFile).getFilePath();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        File downloadFile = downloadPhoto(filePath);
        System.out.println("downloadedFilePath: " + downloadFile);

        try {
            assert downloadFile != null;
            Users.get(chat_id).image = ImageIO.read(downloadFile);
            Users.get(chat_id).file = downloadFile;
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public String getBotUsername () { return "Image_With_Text_Bot"; }

    public String getBotToken () {
        Properties prop = new Properties();
        try {
            prop.load(XmlCollectionJaxbProvider.App.class.getClassLoader().getResourceAsStream("config.properties"));

            return prop.getProperty("token");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
