package alexiil.mods.load;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import alexiil.mods.load.json.*;
import net.minecraft.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.lwjgl.opengl.GL11;

import alexiil.mods.load.ProgressDisplayer.IDisplayer;
import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.FMLFolderResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

public class MinecraftDisplayer implements IDisplayer {

    private String title = "betterloadingscreen:textures/transparent.png";
    private String background = "betterloadingscreen:textures/backgrounds/background1.png";
    private String loadingBarsColor = "e58c1c"; //Color for progress bar
    private String textColor = "ffffff"; // Color for text
    private String GTprogress = "betterloadingscreen:textures/mainProgressBar.png";
    private String progress = "betterloadingscreen:textures/mainProgressBar.png";
    private static String defaultSound = "";
    private static String sound;
    private static String fontTexture;
    private static String defaultFontTexture = "textures/font/ascii.png";
    private ImageRender[] images;
    private Minecraft mc = null;
    private final boolean preview;
    private TextureManager textureManager = null;
    private Map<String, FontRenderer> fontRenderers = new HashMap<String, FontRenderer>();
    private FontRenderer fontRenderer = null;
    private ScaledResolution resolution = null;
    private boolean callAgain = false;
    private IResourcePack myPack;
    private float clearRed = 1, clearGreen = 1, clearBlue = 1;
    private boolean hasSaidNice = false;
    public static float lastPercent = 0;
    private List<String> alreadyUsedBGs = new ArrayList<>();
    private List<String> alreadyUsedTooltips = new ArrayList<>();

    private int[] titlePos = new int[] {
              0, //xLocation
              0, //yLocation
            256, //xWidth
            256, //yWidth
              0, //xLocation
             50, //yLocation
            187, //xWidth
            145  //yWidth
    };
    private int[] GTprogressPos = new int[] {
              0, //xLocation
              0, //yLocation
            194, //xWidth
             24, //yWidth
              0, //xLocation
            -83, //yLocation
            188, //xWidth
             12  //yWidth
    };
    private int[] GTprogressPosAnimated = GTprogressPos;
    private int[] progressPos = new int[] {
              0, //xLocation
              0, //yLocation
            194, //xWidth
             24, //yWidth
              0, //xLocation
            -50, //yLocation
            194, //xWidth
             16  //yWidth
    };
    private int[] progressPosAnimated = new int[] {
              0, //xLocation
             24, //yLocation
            194, //xWidth
             24, //yWidth
              0, //xLocation
            -50, //yLocation
            194, //xWidth
             16  //yWidth
    };
    private int[] progressTextPos = new int[] {
               0, //x
             -20  //y
    };
    private int[] progressPercentagePos = new int[] {
              0, //x
            -35  //y
    };
    private int[] GTprogressTextPos = new int[] {
              0, //x
            -65  //y
    };
    private int[] GTprogressPercentagePos = new int[] {
              0, //x
            -75  //y
    };
    private boolean textShadow = true;

    private boolean randomBackgrounds  = true;
    public static String[] randomBackgroundArray = new String[] {
            "betterloadingscreen:textures/backgrounds/bg1.jpg",
            "betterloadingscreen:textures/backgrounds/bg2.jpg",
            "betterloadingscreen:textures/backgrounds/bg3.jpg",
            "betterloadingscreen:textures/backgrounds/bg4.jpg",
            "betterloadingscreen:textures/backgrounds/bg5.jpg",
            "betterloadingscreen:textures/backgrounds/bg6.jpg",
            "betterloadingscreen:textures/backgrounds/bg7.jpg",
            "betterloadingscreen:textures/backgrounds/bg8.jpg",
            "betterloadingscreen:textures/backgrounds/bg9.jpg",
            "betterloadingscreen:textures/backgrounds/bg10.jpg",
            "betterloadingscreen:textures/backgrounds/bg11.jpg",
            "betterloadingscreen:textures/backgrounds/bg12.jpg",
            "betterloadingscreen:textures/backgrounds/bg13.jpg",
            "betterloadingscreen:textures/backgrounds/bg14.jpg",
            "betterloadingscreen:textures/backgrounds/bg15.jpg"
    };
    private boolean blendingEnabled = true;
    private int threadSleepTime = 20;
    private int changeFrequency = 40;
    private float alphaDecreaseStep = 0.01F;
    private boolean shouldGLClear = false;
    private boolean salt = false;

    private float[] lbRGB = new float[] {1, 1, 0};
    private float loadingBarsAlpha = 0.5F;
    private boolean useImgur = true;
    public static String imgurGalleryLink = "";

    private boolean saltBGhasBeenRendered = false;

    public static boolean isNice = false;
    public static boolean isRegisteringGTmaterials = false;
    public static boolean isReplacingVanillaMaterials = false;
    public static boolean blending = false;
    public static boolean blendingJustSet = false;
    public static float blendAlpha = 1F;
    private static String newBlendImage = "none";
    private static int nonStaticElementsToGo;
    private static Logger log = LogManager.getLogger("betterloadingscreen");

    private ScheduledExecutorService backgroundExec = null;

    private ScheduledExecutorService tipExec = null;
    private boolean scheduledBackgroundExecSet = false;


    private boolean experimental = false;

    public static void playFinishedSound() {
        SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
        ResourceLocation location = new ResourceLocation(sound);
        SoundEventAccessorComposite snd = soundHandler.getSound(location);
        if (snd == null) {
            log.warn("The sound given (" + sound + ") did not give a valid sound!");
            location = new ResourceLocation(defaultSound);
            snd = soundHandler.getSound(location);
        }
        if (snd == null) {
            log.warn("Default sound did not give a valid sound!");
            return;
        }
        ISound sound = PositionedSoundRecord.func_147673_a(location);
        soundHandler.playSound(sound);
    }

    public MinecraftDisplayer() {
        this(false);
    }

    public MinecraftDisplayer(boolean preview) {
        this.preview = preview;
    }

    @SuppressWarnings("unchecked")
    private List<IResourcePack> getOnlyList() {
        Field[] flds = mc.getClass().getDeclaredFields();
        for (Field f : flds) {
            if (f.getType().equals(List.class) && !Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                try {
                    return (List<IResourcePack>) f.get(mc);
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void openPreview(ImageRender[] renders) {
        mc = Minecraft.getMinecraft();
        images = renders;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public String randomBackground(String currentBG) {
        if (randomBackgroundArray.length == 1){
            return randomBackgroundArray[0];
        }
    	//System.out.println("currentBG is: "+currentBG);
    	Random rand = new Random();
    	String res = randomBackgroundArray[rand.nextInt(randomBackgroundArray.length)];
        //System.out.println("New res is: "+res);
        //System.out.println("Does alreadyUsedBGs contain res?: "+String.valueOf(alreadyUsedBGs.contains(res)));
        if (randomBackgroundArray.length == alreadyUsedBGs.size()) {
            alreadyUsedBGs.clear();
        }
    	while (res.equals(currentBG) || alreadyUsedBGs.contains(res)) {
    		res = randomBackgroundArray[rand.nextInt(randomBackgroundArray.length)];
    		//System.out.println("Rerolled res is: "+res);
    	}
        alreadyUsedBGs.add(res);
    	//System.out.println("res is: "+res);
    	return res;
    }

    // Minecraft's display hasn't been created yet, so don't bother trying
    // to do anything now
    @Override
    public void open(Configuration cfg) {
        mc = Minecraft.getMinecraft();
        String n = System.lineSeparator();
        // Open the normal config

        String comment4 = "What font texture to use? Special Cases:"
                + n +" - If you use the Russian mod \"Client Fixer\" then change this to \"textures/font/ascii_fat.png\"" + n +
                "Note: if a resourcepack adds a font, it will be used by CLS.";
        fontTexture = cfg.getString("font", "general", defaultFontTexture, comment4);

        String comment25 = "Time in milliseconds between each image change (smooth blend)."+ n +
        		"The animation runs on the main thread (because OpenGL bruh momento), so setting this higher than"+n+
        		"default is not recommended (basically: if image transition running, your mods not loading)";
        threadSleepTime = cfg.getInt("threadSleepTime", "changing background", threadSleepTime, 0, 9000, comment25);
        /*
        NOBODY EXPECTS THE SPANISH INQUISITION!
         */
        String comment26 = "Wach how many seconds the background should change";
        changeFrequency = cfg.getInt("changeFrequency", "changing background", changeFrequency, 1, 9000, comment26);
        String comment27 = "Float from 0 to 1. The amount of alpha that is removed from the original image and added to the image that comes after."+ n +
        		"Also defined smoothnes of animation. Don't set this too low this time or you'll add time to your pack loading. Probably "+String.valueOf(alphaDecreaseStep)+" still is too low.";
        alphaDecreaseStep = cfg.getFloat("alphaDecreaseStep", "changing background", alphaDecreaseStep, 0, 1, comment27);
        String comment28 = "No, don't touch that!";
        shouldGLClear = cfg.getBoolean("shouldGLClear", "changing background", shouldGLClear, comment28);

        //salt
        String comment29 = "If you want to save a maximum of time on your loading time but don't want to face a black screen, try this.";
        salt = cfg.getBoolean("salt", "skepticism", salt, comment29);

        try {
            lbRGB[0] = (float)(Color.decode("#" + loadingBarsColor).getRed() & 255) / 255.0f;//Color.decode("#" + loadingBarsColor).getRed();
            lbRGB[1] = (float)(Color.decode("#" + loadingBarsColor).getGreen() & 255) / 255.0f;//Color.decode("#" + loadingBarsColor).getGreen();
            lbRGB[2] = (float)(Color.decode("#" + loadingBarsColor).getBlue() & 255) / 255.0f;//Color.decode("#" + loadingBarsColor).getBlue();
            //log.info("The color: " + String.valueOf(lbRGB[0]) + ";" + String.valueOf(lbRGB[1]) + ";" + String.valueOf(lbRGB[2]));
        } catch (Exception e) {
            lbRGB[0] = 1;
            lbRGB[1] = 0.5176471f;
            lbRGB[2] = 0;
            log.warn("Invalid loading bars color");
        }
        /*if (useImgur) {
            System.out.println("2hmmm");
            List<Thread> workers = Stream
                    .generate(() -> new Thread(new DlAllImages(countDownLatch)))
                    .limit(1)
                    .collect(toList());
            workers.forEach(Thread::start);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        if (salt) {
        	blendingEnabled = false;
        }

        // Add ourselves as a resource pack
        if (!preview) {
            if (!ProgressDisplayer.coreModLocation.isDirectory())
                myPack = new FMLFileResourcePack(ProgressDisplayer.modContainer);
            else
                myPack = new FMLFolderResourcePack(ProgressDisplayer.modContainer);
            getOnlyList().add(myPack);
            mc.refreshResources();
        }

        if (randomBackgrounds && !salt) {
            //System.out.println("choosing first random bg");
            Random rand = new Random();
            background = randomBackgroundArray[rand.nextInt(randomBackgroundArray.length)];

            ///timer
            if (!scheduledBackgroundExecSet) {
                //System.out.println("Setting background exec");
                scheduledBackgroundExecSet = true;
                backgroundExec = Executors.newSingleThreadScheduledExecutor();
                backgroundExec.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        if (!blending /*&& !isRegisteringBartWorks && !isRegisteringGTmaterials && !isReplacingVanillaMaterials*/) {
                            alexiil.mods.load.MinecraftDisplayer.blending = true;
                            alexiil.mods.load.MinecraftDisplayer.blendingJustSet = true;
                            alexiil.mods.load.MinecraftDisplayer.blendAlpha = 1;
                        }
                    }
                }, changeFrequency, changeFrequency, TimeUnit.SECONDS);
            }
            ///
        }

        // Open the special config directory
        //File configDir = new File("./config/Betterloadingscreen");
        File configDir = new File("./config");
        /*if (!configDir.exists()) {
            configDir.mkdirs();
        }*/
    }

    @Override
    public void displayProgress(String text, float percent) {
    	if (!salt) {
	    	if (alexiil.mods.load.MinecraftDisplayer.isRegisteringGTmaterials || isReplacingVanillaMaterials) {
	    	    images = new ImageRender[11];
	    	    nonStaticElementsToGo = 10;
	    		//background
	    		if (!background.equals("")) {
	    			images[0] = new ImageRender(background, EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 0, 0));
				} else {
                    images[0] = new ImageRender("betterloadingscreen:textures/transparent.png", EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 10, 10));
                }
	    		//Logo
	    		if (!title.equals("")) {
					images[1] = new ImageRender(title, EPosition.CENTER, EType.STATIC, new Area(titlePos[0], titlePos[1], titlePos[2], titlePos[3]), new Area(titlePos[4], titlePos[5], titlePos[6], titlePos[7]));
				} else {
					images[1] = new ImageRender("betterloadingscreen:textures/transparent.png", EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 10, 10));
				}
	    		//GT progress text
	            images[2] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_STATUS, null, new Area(GTprogressTextPos[0], GTprogressTextPos[1], 0, 0), "ffffff", null, "");
                //GT progress percentage text
	    		images[3] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_PERCENTAGE, null, new Area(GTprogressPercentagePos[0], GTprogressPercentagePos[1], 0, 0), "ffffff", null, "");
	            //Static NORMAL bar image
	            images[4] = new ImageRender(progress, EPosition.CENTER, EType.STATIC, new Area(progressPos[0], progressPos[1], progressPos[2], progressPos[3]), new Area(progressPos[4], progressPos[5], progressPos[6], progressPos[7]));
	            //Dynamic NORMAL bar image (yellow thing)
	            images[5] = new ImageRender(progress, EPosition.CENTER, EType.DYNAMIC_PERCENTAGE, new Area(progressPosAnimated[0], progressPosAnimated[1], progressPosAnimated[2], progressPosAnimated[3]), new Area(progressPosAnimated[4], progressPosAnimated[5], progressPosAnimated[6], progressPosAnimated[7]));
	            //NORMAL progress text
	            images[6] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_STATUS, null, new Area(progressTextPos[0], progressTextPos[1], 0, 0), "ffffff", null, "");
	            //NORMAL progress percentage text
	            images[7] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_PERCENTAGE, null, new Area(progressPercentagePos[0], progressPercentagePos[1], 0, 0), "ffffff", null, "");
	            //Static GT bar image
	            images[8] = new ImageRender(GTprogress, EPosition.CENTER, EType.STATIC, new Area(GTprogressPos[0], GTprogressPos[1], GTprogressPos[2], GTprogressPos[3]), new Area(GTprogressPos[4], GTprogressPos[5], GTprogressPos[6], GTprogressPos[7]));
                //Dynamic GT bar image (yellow thing)
	            images[9] = new ImageRender(GTprogress, EPosition.CENTER, EType.DYNAMIC_PERCENTAGE, new Area(GTprogressPosAnimated[0], GTprogressPosAnimated[1], GTprogressPosAnimated[2], GTprogressPosAnimated[3]), new Area(GTprogressPosAnimated[4], GTprogressPosAnimated[5], GTprogressPosAnimated[6], GTprogressPosAnimated[7]));

                //Hmmm no idea what that is, maybe the thing that clears the screen
                images[10] = new ImageRender(null, null, EType.CLEAR_COLOUR, null, null, "ffffff", null, "");

            }	else {

	    	    images = new ImageRender[7];
	    	    nonStaticElementsToGo = 6;

                //background
				if (!background.equals("")) {
	    			images[0] = new ImageRender(background, EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 0, 0));
				} else {
					images[0] = new ImageRender("betterloadingscreen:textures/transparent.png", EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 10, 10));
				}
				//Logo
				if (!title.equals("")) {
					images[1] = new ImageRender(title, EPosition.CENTER, EType.STATIC, new Area(titlePos[0], titlePos[1], titlePos[2], titlePos[3]), new Area(titlePos[4], titlePos[5], titlePos[6], titlePos[7]));
				} else {
					images[1] = new ImageRender("betterloadingscreen:textures/transparent.png", EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 10, 10));
				}
                //NORMAL progress text
	            images[2] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_STATUS, null, new Area(progressTextPos[0], progressTextPos[1], 0, 0), "ffffff", null, "");
                //NORMAL progress percentage text
				images[3] = new ImageRender(fontTexture, EPosition.CENTER, EType.DYNAMIC_TEXT_PERCENTAGE, null, new Area(progressPercentagePos[0], progressPercentagePos[1], 0, 0), "ffffff", null, "");
                //Static NORMAL bar image
	            images[4] = new ImageRender(progress, EPosition.CENTER, EType.STATIC, new Area(progressPos[0], progressPos[1], progressPos[2], progressPos[3]), new Area(progressPos[4], progressPos[5], progressPos[6], progressPos[7]));
                //Dynamic NORMAL bar image (yellow thing)
	            images[5] = new ImageRender(progress, EPosition.CENTER, EType.DYNAMIC_PERCENTAGE, new Area(progressPosAnimated[0], progressPosAnimated[1], progressPosAnimated[2], progressPosAnimated[3]), new Area(progressPosAnimated[4], progressPosAnimated[5], progressPosAnimated[6], progressPosAnimated[7]));

	            images[6] = new ImageRender(null, null, EType.CLEAR_COLOUR, null, null, "ffffff", null, "");

	    	}
    	} else {
    		shouldGLClear = false;
    		textShadow = false;
    		textColor = "000000";
    		if (!saltBGhasBeenRendered) {
    			images = new ImageRender[2];
    			images[0] = new ImageRender("betterloadingscreen:textures/salt.png", EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 0, 0));
    			images[1] = new ImageRender(fontTexture, EPosition.BOTTOM_LEFT, EType.DYNAMIC_TEXT_STATUS, null, new Area(10, 10, 0, 0), "000000", null, "");
			} else {
				images = new ImageRender[0];
			}
    	}

        resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        preDisplayScreen();

        int imageCounter = 0;

        if (!isRegisteringGTmaterials && !isReplacingVanillaMaterials) {
			lastPercent = percent;
		}

        for (ImageRender image : images) {
//        	if (!usingGT) {
//				lastPercent = percent;
//			}
        	if (salt) {
        		drawImageRender(image, "Minecraft is loading, please wait...", percent);
			} else if (image != null && !((isRegisteringGTmaterials || isReplacingVanillaMaterials) && imageCounter > 4 && (isRegisteringGTmaterials || isReplacingVanillaMaterials) && imageCounter < 9)) {
                drawImageRender(image, text, percent);
            } else if (image != null && isRegisteringGTmaterials && !isNice) {
            	drawImageRender(image," Post Initialization: Registering Gregtech materials", lastPercent);

			} else if (image != null && isRegisteringGTmaterials && isNice) {
            	drawImageRender(image," Post Initialization: Registering nice Gregtech materials", lastPercent);
            	if(!hasSaidNice) {
            		hasSaidNice = true;
            		log = LogManager.getLogger("betterloadingscreen");
            		log.info("Yeah, that's nice, funni number");
            	}
			} else if (isReplacingVanillaMaterials) {
				drawImageRender(image," Post Initialization: Gregtech replacing Vanilla materials in recipes", lastPercent);
			}
            imageCounter++;
        }

        postDisplayScreen();

        if (callAgain) {
            // For some reason, calling this again makes pre-init render properly. I have no idea why, it just does
            callAgain = false;
            displayProgress(text, percent);
        }
    }

    private FontRenderer fontRenderer(String fontTexture) {
        if (fontRenderers.containsKey(fontTexture)) {
            return fontRenderers.get(fontTexture);
        }
        FontRenderer font = new FontRenderer(mc.gameSettings, new ResourceLocation(fontTexture), textureManager, false);
        font.onResourceManagerReload(mc.getResourceManager());
        if (!preview) {
            mc.refreshResources();
            font.onResourceManagerReload(mc.getResourceManager());
        }
        fontRenderers.put(fontTexture, font);
        return font;
    }

    public void drawImageRender(ImageRender render, String text, double percent) {
        int startX = render.transformX(resolution.getScaledWidth());
        int startY = render.transformY(resolution.getScaledHeight());
        int PWidth = 0;
        int PHeight = 0;
        int intColor = Integer.parseInt(textColor, 16);
        if (render.position != null) {
            PWidth = render.position.width == 0 ? resolution.getScaledWidth() : render.position.width;
            PHeight = render.position.height == 0 ? resolution.getScaledHeight() : render.position.height;
        }
        GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), 1);
        switch (render.type) {
            case DYNAMIC_PERCENTAGE: {
                ResourceLocation res = new ResourceLocation(render.resourceLocation);
                textureManager.bindTexture(res);
                double visibleWidth = PWidth * percent;
                double textureWidth = render.texture.width * percent;
                GL11.glColor4f(lbRGB[0], lbRGB[1], lbRGB[2], loadingBarsAlpha);
                drawRect(startX, startY, visibleWidth, PHeight, render.texture.x, render.texture.y, textureWidth, render.texture.height);
                GL11.glColor4f(1, 1, 1, 1);
                break;
            }
            case DYNAMIC_TEXT_PERCENTAGE: {
                FontRenderer font = fontRenderer(render.resourceLocation);
                String percentage = (int) (percent * 100) + "%";
                int width = font.getStringWidth(percentage);
                startX = render.positionType.transformX(render.position.x, resolution.getScaledWidth() - width);
                startY = render.positionType.transformY(render.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                if (textShadow) {
                	font.drawStringWithShadow(percentage, startX, startY, /*render.getColour()*/intColor);
				} else {
					drawString(font, percentage, startX, startY, intColor);
				}
                break;
            }
            case DYNAMIC_TEXT_STATUS: {
                FontRenderer font = fontRenderer(render.resourceLocation);
                int width = font.getStringWidth(text);
                startX = render.positionType.transformX(render.position.x, resolution.getScaledWidth() - width);
                startY = render.positionType.transformY(render.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                ////////////////
                //This allows to draw each char separately.
                if (experimental) {
                    int currentX = startX;
                    for (int i = 0; i < text.length(); i++) {
                        //drawString(font., String.valueOf(text.charAt(i)), currentX, startY, intColor);
                        double scale = 2;
                        log.info("currentX before scale: " + currentX);
                        GL11.glScaled(scale, scale, scale);
                        log.info("currentX after scale: " + currentX);
                        drawString(font, String.valueOf(text.charAt(i)), (int) (currentX / scale), (int) (startY / scale), /*intColor*/0);
                        GL11.glScaled(1, 1, 1);
                        currentX += font.getCharWidth(text.charAt(i));
                    }
                }
                ///////////////
                else {
                    if (textShadow) {
                        font.drawStringWithShadow(text, startX, startY, intColor);
                    } else {
                        drawString(font, text, startX, startY, intColor);
                    }
                }
                break;
            }
            case STATIC_TEXT: {
                FontRenderer font = fontRenderer(render.resourceLocation);
                int width = font.getStringWidth(render.text);
                int startX1 = render.positionType.transformX(render.position.x, resolution.getScaledWidth() - width);
                int startY1 = render.positionType.transformY(render.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                if (textShadow) {
                	font.drawStringWithShadow(render.text, startX1, startY1, intColor);
				} else {
					drawString(font, render.text, startX1, startY1, intColor);
				}
				break;
            }
            case TIPS_TEXT: {
                FontRenderer font = fontRenderer(render.resourceLocation);
                int width = font.getStringWidth(render.text);
                int startX1 = render.positionType.transformX(render.position.x, resolution.getScaledWidth() - width);
                //System.out.println("startX1 normal: "+startX1);
                int startY1 = render.positionType.transformY(render.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                break;
            }
            case STATIC: {
            	if (blending) {
            		preDisplayScreen();
            		GL11.glClearColor(clearRed, clearGreen, clearBlue, 1);
            		//GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            		if (blendingJustSet) {
            			blendingJustSet = false;
            		    //System.out.println("start blend");
            			Random rand = new Random();
            			newBlendImage = randomBackground(render.resourceLocation);//randomBackgroundArray[rand.nextInt(randomBackgroundArray.length)];
            		}

            		GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), blendAlpha);//+0.1F);

            		blendAlpha -= alphaDecreaseStep;
            		//System.out.println("blendAlpha: "+blendAlpha);
            		if (blendAlpha <= 0) {
						blending = false;
						background = newBlendImage;
					}
            		try {
						Thread.sleep(threadSleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

            		ResourceLocation res = new ResourceLocation(render.resourceLocation);
                    textureManager.bindTexture(res);
                    drawRect(startX, startY, PWidth, PHeight, render.texture.x, render.texture.y, render.texture.width, render.texture.height);
                    //drawImageRender(render, text, percent);

                    ImageRender render2 = new ImageRender(newBlendImage, EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 256, 256));
                    GL11.glColor4f(render2.getRed(), render2.getGreen(), render2.getBlue(), 1-blendAlpha-0.05F);//+0.01F);
                    ResourceLocation res2 = new ResourceLocation(render2.resourceLocation);
                    textureManager.bindTexture(res2);
                    drawRect(startX, startY, PWidth, PHeight, render2.texture.x, render2.texture.y, render2.texture.width, render2.texture.height);
                    //drawImageRender(render2, text, percent);

                    //Rest of the images

                    //loading bar static
                    GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), 1F);
                    ImageRender render3 = new ImageRender(images[4].resourceLocation, images[4].positionType, images[4].type, images[4].texture, images[4].position);
                    startX = progressPos[0];//render3.transformX(resolution.getScaledWidth());
                    startY = progressPos[1];//render3.transformY(resolution.getScaledHeight());
                    ResourceLocation res3 = new ResourceLocation(images[4].resourceLocation);
                    textureManager.bindTexture(res3);
                    /*double visibleWidth = PWidth * percent;
                    double textureWidth = render.texture.width * percent;*/
                    startX = render3.transformX(resolution.getScaledWidth());
                    startY = render3.transformY(resolution.getScaledHeight());
                    PWidth = 0;
                    PHeight = 0;
                    if (render3.position != null) {
                        PWidth = render3.position.width == 0 ? resolution.getScaledWidth() : render3.position.width;
                        PHeight = render3.position.height == 0 ? resolution.getScaledHeight() : render3.position.height;
                    }
                    drawRect(startX, startY,PWidth, PHeight, render3.texture.x, render3.texture.y, render3.texture.width, render3.texture.height);
                    //loading bar animated
                    ImageRender render4 = new ImageRender(images[5].resourceLocation, images[5].positionType, images[5].type, images[5].texture, images[5].position);

                    //startX = progressPos[0];//render3.transformX(resolution.getScaledWidth());
                    //startY = progressPos[1];//render3.transformY(resolution.getScaledHeight());
                    ResourceLocation res4 = new ResourceLocation(images[5].resourceLocation);
                    textureManager.bindTexture(res4);
                    /*double visibleWidth = PWidth * percent;
                    double textureWidth = render.texture.width * percent;*/
                    startX = render4.transformX(resolution.getScaledWidth());
                    startY = render4.transformY(resolution.getScaledHeight());
                    PWidth = 0;
                    PHeight = 0;
                    if (render4.position != null) {
                        PWidth = render4.position.width == 0 ? resolution.getScaledWidth() : render4.position.width;
                        PHeight = render4.position.height == 0 ? resolution.getScaledHeight() : render4.position.height;
                    }
                    //hmmm test
                    double visibleWidth;
                    double textureWidth;
                    if (isRegisteringGTmaterials || isReplacingVanillaMaterials) {
                        visibleWidth = PWidth * lastPercent;
                        textureWidth = render4.texture.width * lastPercent;
                    } else {
                        visibleWidth = PWidth * percent;
                        textureWidth = render4.texture.width * percent;
                    }
                    ///
                    GL11.glColor4f(lbRGB[0], lbRGB[1], lbRGB[2], loadingBarsAlpha);
                    drawRect(startX, startY, visibleWidth, PHeight, render4.texture.x, render4.texture.y, textureWidth, render4.texture.height);
                    GL11.glColor4f(1, 1, 1, 1);
                    //dynamic text
                    ImageRender render5 = new ImageRender(images[2].resourceLocation, images[2].positionType, images[2].type, images[2].texture, images[2].position);
                    FontRenderer font = fontRenderer(render5.resourceLocation);
                    int width;
                    if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                        width = font.getStringWidth(" Post Initialization: Registering Gregtech materials");
                    } else {
                        width = font.getStringWidth(text);
                    }
                    //System.out.println("width1 is: "+String.valueOf(width));
                    startX = render5.positionType.transformX(render5.position.x, resolution.getScaledWidth() - width);
                    startY = render5.positionType.transformY(render5.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                    if (textShadow) {
                        if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                            font.drawStringWithShadow(" Post Initialization: Registering Gregtech materials", startX, startY, intColor);
                        } else {
                            font.drawStringWithShadow(text, startX, startY, intColor);
                        }
                    } else {
                        if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                            drawString(font, " Post Initialization: Registering Gregtech materials", startX, startY, intColor);
                        } else {
                            drawString(font, text, startX, startY, intColor);
                        }
                    }
                    //dynamic text percentage
                    ImageRender render6 = new ImageRender(images[3].resourceLocation, images[3].positionType, images[3].type, images[3].texture, images[3].position);
                    String percentage = (int) (percent * 100) + "%";
                    if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                        width = font.getStringWidth(String.valueOf(lastPercent));
                    } else {
                        width = font.getStringWidth(percentage);
                    }
                    startX = render6.positionType.transformX(render6.position.x, resolution.getScaledWidth() - width);
                    startY = render6.positionType.transformY(render6.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                    if (textShadow) {
                        if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                            font.drawStringWithShadow(String.valueOf((int)(lastPercent*100)), startX, startY, /*render.getColour()*/intColor);
                        } else {
                            font.drawStringWithShadow(percentage, startX, startY, /*render.getColour()*/intColor);
                        }
    				} else {
                        if (false/*isRegisteringGTmaterials || isReplacingVanillaMaterials || isRegisteringBartWorks*/) {
                            drawString(font, String.valueOf((int)lastPercent*100), startX, startY, intColor);
                        } else {
                            drawString(font, percentage, startX, startY, intColor);
                        }
    				}


                    ///////////
                    //GT
                    if (isRegisteringGTmaterials || isReplacingVanillaMaterials) {
                        //loading bar static
                        GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), 1F);
                        ImageRender render7 = new ImageRender(images[8].resourceLocation, images[8].positionType, images[8].type, images[8].texture, images[8].position);
                        startX = progressPos[0];//render3.transformX(resolution.getScaledWidth());
                        startY = progressPos[1];//render3.transformY(resolution.getScaledHeight());
                        ResourceLocation res7 = new ResourceLocation(images[8].resourceLocation);
                        textureManager.bindTexture(res3);
                        startX = render7.transformX(resolution.getScaledWidth());
                        startY = render7.transformY(resolution.getScaledHeight());
                        PWidth = 0;
                        PHeight = 0;
                        if (render7.position != null) {
                            PWidth = render7.position.width == 0 ? resolution.getScaledWidth() : render7.position.width;
                            PHeight = render7.position.height == 0 ? resolution.getScaledHeight() : render7.position.height;
                        }
                        drawRect(startX, startY,PWidth, PHeight, render7.texture.x, render7.texture.y, render7.texture.width, render7.texture.height);
                        //loading bar animated
                        //GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), 1F);
                        ImageRender render8 = new ImageRender(images[9].resourceLocation, images[9].positionType, images[9].type, images[9].texture, images[9].position);
                        ResourceLocation res8 = new ResourceLocation(images[9].resourceLocation);
                        textureManager.bindTexture(res8);
                        startX = render8.transformX(resolution.getScaledWidth());
                        startY = render8.transformY(resolution.getScaledHeight());
                        PWidth = 0;
                        PHeight = 0;
                        if (render4.position != null) {
                            PWidth = render8.position.width == 0 ? resolution.getScaledWidth() : render8.position.width;
                            PHeight = render8.position.height == 0 ? resolution.getScaledHeight() : render8.position.height;
                        }
                        visibleWidth = PWidth * percent;
                        textureWidth = render8.texture.width * percent;
                        GL11.glColor4f(lbRGB[0], lbRGB[1], lbRGB[2], loadingBarsAlpha);
                        drawRect(startX, startY, visibleWidth, PHeight, render8.texture.x, render8.texture.y, textureWidth, render8.texture.height);
                        GL11.glColor4f(1, 1, 1, 1);
                        //dynamic text
                        ImageRender render9 = new ImageRender(images[6].resourceLocation, images[6].positionType, images[6].type, images[6].texture, images[6].position);
                        font = fontRenderer(render9.resourceLocation);
                        width = font.getStringWidth(" Post Initialization: Registering Gregtech materials");
                        startX = render9.positionType.transformX(render9.position.x, resolution.getScaledWidth() - width);
                        startY = render9.positionType.transformY(render9.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                        if (textShadow) {
                            font.drawStringWithShadow(" Post Initialization: Registering Gregtech materials", startX, startY, intColor);
                        } else {
                            drawString(font, " Post Initialization: Registering Gregtech materials", startX, startY, intColor);
                        }
                        //dynamic text percentage
                        ImageRender render10 = new ImageRender(images[7].resourceLocation, images[7].positionType, images[7].type, images[7].texture, images[7].position);
                        percentage = (int) (percent * 100) + "%";
                        width = font.getStringWidth(String.valueOf((int)lastPercent*100) + "%");
                        startX = render10.positionType.transformX(render10.position.x, resolution.getScaledWidth() - width);
                        startY = render10.positionType.transformY(render10.position.y, resolution.getScaledHeight() - font.FONT_HEIGHT);
                        if (textShadow) {
                            //System.out.println("lastPercent: "+String.valueOf(lastPercent));
                            font.drawStringWithShadow(String.valueOf((int)(lastPercent*100)) + "%", startX, startY, /*render.getColour()*/intColor);
                        } else {
                            drawString(font, String.valueOf((int)(lastPercent*100)) + "%", startX, startY, intColor);
                        }
                    }

                    postDisplayScreen();
                    drawImageRender(render, text, percent);
            		break;
            	} else {
            		if (!newBlendImage.contentEquals("none")) {

						render = new ImageRender(newBlendImage, EPosition.TOP_LEFT, EType.STATIC, new Area(0, 0, 256, 256), new Area(0, 0, 256, 256));
						newBlendImage = "none";
            		}
            		GL11.glColor4f(render.getRed(), render.getGreen(), render.getBlue(), 1F);
            		ResourceLocation res = new ResourceLocation(render.resourceLocation);
                    textureManager.bindTexture(res);
                    drawRect(startX, startY, PWidth, PHeight, render.texture.x, render.texture.y, render.texture.width, render.texture.height);
                    break;
            	}

                //break;
            }
            case CLEAR_COLOUR:// Ignore this, as its set elsewhere
                break;
        }
    }

    public void drawString(FontRenderer font, String text, int x, int y, int colour) {
        font.drawString(text, x, y, colour);
        GL11.glColor4f(1, 1, 1, 1);
    }

    public void drawRect(double x, double y, double drawnWidth, double drawnHeight, double u, double v, double uWidth, double vHeight) {
        float f = 1 / 256F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + drawnHeight, 0, u * f, (v + vHeight) * f);
        tessellator.addVertexWithUV(x + drawnWidth, y + drawnHeight, 0, (u + uWidth) * f, (v + vHeight) * f);
        tessellator.addVertexWithUV(x + drawnWidth, y, 0, (u + uWidth) * f, v * f);
        tessellator.addVertexWithUV(x, y, 0, u * f, v * f);
        tessellator.draw();
    }

    private void preDisplayScreen() {
    	//System.out.println("Called preDisplayScreen");
    	//bruh
        if (textureManager == null) {
            if (preview) {
                textureManager = mc.renderEngine;
            }
            else {
                textureManager = mc.renderEngine = new TextureManager(mc.getResourceManager());
                mc.refreshResources();
                textureManager.onResourceManagerReload(mc.getResourceManager());
                mc.fontRenderer = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), textureManager, false);
                if (mc.gameSettings.language != null) {
                    mc.fontRenderer.setUnicodeFlag(mc.func_152349_b());
                    LanguageManager lm = mc.getLanguageManager();
                    mc.fontRenderer.setBidiFlag(lm.isCurrentLanguageBidirectional());
                }
                mc.fontRenderer.onResourceManagerReload(mc.getResourceManager());
                callAgain = true;
            }
        }
        if (fontRenderer != mc.fontRenderer) {
            fontRenderer = mc.fontRenderer;
        }
        // if (textureManager != mc.renderEngine)
        // textureManager = mc.renderEngine;
        resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int i = resolution.getScaleFactor();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, (double) resolution.getScaledWidth(), (double) resolution.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glClearColor(clearRed, clearGreen, clearBlue, 1);
        //EXPERIMENTAL!! - DISABLING THE WHITE CLEAT - EXPERIMENTAL!!!!
        if (shouldGLClear) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		}

        GL11.glEnable(GL11.GL_BLEND);
        //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_ALPHA_TEST);
        //GL11.glEnable(1);
        //GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        GL11.glColor4f(1, 1, 1, 1);

        //System.out.println("alpha: "+GL11.GL_ALPHA);
        //GL11.GL_ALPHA = 1000;
    }

    public ImageRender[] getImageData() {
        return images;
    }

    private void postDisplayScreen() {
        mc.func_147120_f();
    }

    @Override
    public void close() {
        //System.out.println("closing askip");
        if (tipExec != null) {
            tipExec.shutdown();
        }
        if (backgroundExec != null) {
            backgroundExec.shutdown();
        }
        getOnlyList().remove(myPack);
    }
}
