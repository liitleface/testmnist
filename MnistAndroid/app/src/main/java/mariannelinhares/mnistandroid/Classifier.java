package mariannelinhares.mnistandroid;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class Classifier {

    // Only returns if at least this confidence
    private static final float THRESHOLD = 0.1f;
    private static final String TAG = "Classifier";
    private TensorFlowInferenceInterface tfHelper;

    private String inputName;
    private String outputName;
    private int inputSize;

    private List<String> labels;
    private float[] output;
    private String[] outputNames;

    static private List<String> readLabels(Classifier c, AssetManager am, String fileName) throws IOException {
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }

    static {
        //加载libtensorflow_inference.so库文件
        System.loadLibrary("tensorflow_inference");
        Log.e(TAG,"libtensorflow_inference.so库加载成功");
    }

    static public Classifier create(AssetManager assetManager, String modelPath, String labelPath,
                             int inputSize, String inputName, String outputName)
                             throws IOException {

        Classifier c = new Classifier();

        c.inputName = inputName;
        c.outputName = outputName;

        // Read labels
        String labelFile = labelPath.split("file:///android_asset/")[1];
        //String labelFile = "file:///android_asset/mnist.pb";

        c.labels = readLabels(c, assetManager, labelFile);
       //初始化接口
        c.tfHelper = new TensorFlowInferenceInterface();
        c.tfHelper.initializeTensorFlow(assetManager, modelPath);
        if (c.tfHelper.initializeTensorFlow(assetManager, modelPath) != 0) {
            throw new RuntimeException("TF initialization failed");
        }
        Log.e(TAG,"TensoFlow模型文件加载成功");

        int numClasses = 10;

        c.inputSize = inputSize;

        // Pre-allocate buffer.
        c.outputNames = new String[]{ outputName };

        c.outputName = outputName;
        c.output = new float[numClasses];

        return c;
    }

    public Classification recognize(final float[] pixels) {

        tfHelper.fillNodeFloat(inputName, new int[]{inputSize * inputSize}, pixels);
        tfHelper.runInference(outputNames);

        tfHelper.readNodeFloat(outputName, output);

        // 寻找最佳分类
        Classification ans = new Classification();
        for (int i = 0; i < output.length; ++i) {
            System.out.println(output[i]);
            System.out.println(labels.get(i));
            if (output[i] > THRESHOLD && output[i] > ans.getConf()) {
                ans.update(output[i], labels.get(i));
            }
        }

        return ans;
    }
}
