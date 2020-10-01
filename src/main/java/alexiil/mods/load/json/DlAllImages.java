package alexiil.mods.load.json;

import alexiil.mods.load.MinecraftDisplayer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DlAllImages implements Runnable {

    public DlAllImages (CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    private CountDownLatch countDownLatch;
    @Override
    public void run() {
        //System.out.println("hmmmmmm");
        List<String> images = null;
        try {
            //System.out.println("Getting imgur gallery");
            images = ImgurTest.fetchImgurGallery(MinecraftDisplayer.imgurGalleryLink);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("Got the gallery");
        String[] imageUrls = images.toArray(new String[0]);
        //System.out.println("got here, imageUrls: " + imageUrls.toString());
        //System.out.println("images.length: " + String.valueOf(images.size()));
        List<String> imagePaths = null;
        for (int i = 0; i < imageUrls.length; i++) {
            System.out.println("Downloading " + i + "th image");
            imagePaths.add(ImageDownload.dlImage(imageUrls[i], String.valueOf(i)));
        }
        MinecraftDisplayer.randomBackgroundArray = imagePaths.toArray(new String[0]);
        //System.out.println("bg_array is: "+MinecraftDisplayer.randomBackgroundArray.toString());

        countDownLatch.countDown();
    }
}
