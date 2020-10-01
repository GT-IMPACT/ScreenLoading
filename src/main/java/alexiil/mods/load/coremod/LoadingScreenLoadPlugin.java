package alexiil.mods.load.coremod;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import alexiil.mods.load.ProgressDisplayer;
import alexiil.mods.load.Translation;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.client.SplashProgress;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "alexiil.mods.load.coremod" })
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE - 80)
// A big number
public class LoadingScreenLoadPlugin implements cpw.mods.fml.relauncher.IFMLLoadingPlugin {

	private static Method disableSplashMethodRef;
	static {
	/*try {
	  disableSplashMethodRef = Class.forName("cpw.mods.fml.client.SplashProgress").getMethod("disableSplash");
	  disableSplashMethodRef.setAccessible(true);
	}catch(Exception ignored){}*/
	}
	
    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "alexiil.mods.load.coremod.BetterLoadingScreenTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        File coremodLocation = (File) data.get("coremodLocation");
        Translation.addTranslations(coremodLocation);
        ProgressDisplayer.start(coremodLocation);
        //Field disSplash = SplashProgress.class.getDeclaredField("disableSplash");
        //disSplash.setAccessible(true);
        //cpw.mods.fml.client.SplashProgress.disableSplash();
        //Method method = cpw.mods.fml.client.SplashProgress.disableSplash();//getMethod("doSomething", null);
        //method.setAccessible(true);
        //try {
        	//disableSplashMethodRef.invoke(null);
        //} catch(Exception ignored){}
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
