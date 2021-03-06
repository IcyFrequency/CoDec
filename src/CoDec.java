import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;  
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;  
import java.util.ArrayList;  
import java.util.List;  

import javax.swing.JFrame;  
import javax.swing.JPanel;  

import org.opencv.core.Core;  
import org.opencv.core.Mat;   
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;  
import org.opencv.core.Size;  
import org.opencv.highgui.*;  
import org.opencv.imgproc.*;

import org.opencv.core.CvType;

class Panel extends JPanel{  
	private static final long serialVersionUID = 1L;  
	private BufferedImage image;    
	// Create a constructor method  
	public Panel(){  
		super();  
	}  
	private BufferedImage getimage(){  
		return image;  
	}  
	public void setimage(BufferedImage newimage){  
		image=newimage;  
		return;  
	}  
	public void setimagewithMat(Mat newimage){  
		image=this.matToBufferedImage(newimage);  
		return;  
	}  

	public BufferedImage matToBufferedImage(Mat matrix) {  
		int cols = matrix.cols();  
		int rows = matrix.rows();  
		int elemSize = (int)matrix.elemSize();  
		byte[] data = new byte[cols * rows * elemSize];  
		int type;  
		matrix.get(0, 0, data);  
		switch (matrix.channels()) {  
		case 1:  
			type = BufferedImage.TYPE_BYTE_GRAY;  
			break;  
		case 3:  
			type = BufferedImage.TYPE_3BYTE_BGR;  
			// bgr to rgb  
			byte b;  
			for(int i=0; i<data.length; i=i+3) {  
				b = data[i];  
				data[i] = data[i+2];  
				data[i+2] = b;  
			}  
			break;  
		default:  
			return null;  
		}  
		BufferedImage image2 = new BufferedImage(cols, rows, type);  
		image2.getRaster().setDataElements(0, 0, cols, rows, data);  
		return image2;  
	}  
	@Override  
	protected void paintComponent(Graphics g){  
		super.paintComponent(g);  
		//BufferedImage temp=new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);  
		BufferedImage temp=getimage();  
		//Graphics2D g2 = (Graphics2D)g;
		if( temp != null)
			g.drawImage(temp,10,10,temp.getWidth(),temp.getHeight(), this);  
	}  
}  
public class CoDec{

	public static void main(String arg[]){  
		// Load the native library.  
		//int line = 0;
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  
		// It is better to group all frames together so cut and paste to  
		// create more frames is easier  
		try {
			trackGreen();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return;  
	}
	
	private static void trackGreen() throws AWTException{
		//Mouse mouse = new Mouse();
		JFrame frame1 = new JFrame("Camera");  
		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
		frame1.setSize(640,480);  
		frame1.setBounds(0, 0, frame1.getWidth(), frame1.getHeight());  
		Panel panel1 = new Panel();  
		frame1.setContentPane(panel1);  
		frame1.setVisible(true);  

		//-- 2. Read the video stream  
		VideoCapture capture =new VideoCapture(0); 
		//capture.set(10, 0);
		capture.set(3, 1366);
		capture.set(4, 768);
		capture.set(15, -2);
		Mat webcam_image=new Mat();  
		Mat hsv_image=new Mat();  
		Mat thresholded=new Mat();  
		Mat thresholded2=new Mat();  
		capture.read(webcam_image);  
		frame1.setSize(webcam_image.width()+40,webcam_image.height()+60);  
		
		Mat array255=new Mat(webcam_image.height(),webcam_image.width(),CvType.CV_8UC1);  
		array255.setTo(new Scalar(255)); 
		
		Mat distance=new Mat(webcam_image.height(),webcam_image.width(),CvType.CV_8UC1);  
		//new Mat();//new Size(webcam_image.width(),webcam_image.height()),CvType.CV_8UC1);  
		List<Mat> lhsv = new ArrayList<Mat>(3);      
		Mat circles = new Mat(); // No need (and don't know how) to initialize it.  
		// The function later will do it... (to a 1*N*CV_32FC3)
		
		//pink
		Scalar hsv_min = new Scalar(32, 0, 128, 0);  
		Scalar hsv_max = new Scalar(197, 183, 255, 0);
		
		//blue ish
		//Scalar hsv_min = new Scalar(50, 20,20, 0);  
		//Scalar hsv_max = new Scalar(255, 250,250, 0);
		
		//Green
		//Scalar hsv_min = new Scalar(50, 100,100, 0);  
		//Scalar hsv_max = new Scalar(70, 255,255, 0);
		
		//Scalar hsv_min = new Scalar(0,0,125, 0);  
		//Scalar hsv_max = new Scalar(0,0, 255, 0);  
		
		double[] data=new double[3];  
		if( capture.isOpened())  
		{  
			while( true )  
			{  
				capture.read(webcam_image);  
				if( !webcam_image.empty() )  
				{  
					// One way to select a range of colors by Hue  
					Imgproc.cvtColor(webcam_image, hsv_image, Imgproc.COLOR_BGR2HSV);  
					Core.inRange(hsv_image, hsv_min, hsv_max, thresholded);           
					//Core.inRange(hsv_image, hsv_min2, hsv_max2, thresholded2);
					//Core.bitwise_or(thresholded, thresholded2, thresholded); 
					Imgproc.erode(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8,8)));
					Imgproc.dilate(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8)));
					// Notice that the thresholds don't really work as a "distance"  
					// Ideally we would like to cut the image by hue and then pick just  
					// the area where S combined V are largest.  
					// Strictly speaking, this would be something like sqrt((255-S)^2+(255-V)^2)>Range  
					// But if we want to be "faster" we can do just (255-S)+(255-V)>Range  
					// Or otherwise 510-S-V>Range  
					// Anyhow, we do the following... Will see how fast it goes...  
					Core.split(hsv_image, lhsv); // We get 3 2D one channel Mats  
					Mat S = lhsv.get(1);  
					Mat V = lhsv.get(2);  
					Core.subtract(array255, S, S);  
					Core.subtract(array255, V, V);  
					S.convertTo(S, CvType.CV_32F);  
					V.convertTo(V, CvType.CV_32F);  
					Core.magnitude(S, V, distance);  
					Core.inRange(distance,new Scalar(0.0), new Scalar(200.0), thresholded2);  
					Core.bitwise_and(thresholded, thresholded2, thresholded);  
					// Apply the Hough Transform to find the circles  
					Imgproc.GaussianBlur(thresholded, thresholded, new Size(9,9),0,0);  
					List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
					Imgproc.HoughCircles(thresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, thresholded.height()/8, 200, 100, 0, 0);   
					Imgproc.findContours(thresholded, contours, thresholded2, Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
					Imgproc.drawContours(webcam_image, contours, -1, new Scalar(255, 0, 0), 2);   
					//System.out.println(contours.size());
					//Imgproc.Canny(thresholded, thresholded, 500, 250);  
					//-- 4. Add some info to the image  
					Core.line(webcam_image, new Point(150,50), new Point(202,200), new Scalar(100,10,10)/*CV_BGR(100,10,10)*/, 3);  
					Core.circle(webcam_image, new Point(210,210), 10, new Scalar(100,10,10),3);  
					data=webcam_image.get(210, 210);  
					Core.putText(webcam_image,String.format("("+String.valueOf(data[0])+","+String.valueOf(data[1])+","+String.valueOf(data[2])+")"),new Point(30, 30) , 3 //FONT_HERSHEY_SCRIPT_SIMPLEX  
							,1.0,new Scalar(100,10,10,255),3); 
					int thickness = 2;
					int lineType = 8;
					Point start = new Point(0,0);
					Point end=new Point(0,0);
					Scalar black = new Scalar( 100, 10, 10 );
					//getCoordinates(thresholded);  
					
					int cols = circles.cols();  
					int rows = circles.rows();  
					int elemSize = (int)circles.elemSize(); // Returns 12 (3 * 4bytes in a float)  
					float[] data2 = new float[rows * elemSize/4];  
					//hei
					Core.line(hsv_image, new Point(150,50), new Point(202,200), new Scalar(100,10,10)/*CV_BGR(100,10,10)*/, 3);  
					Core.circle(hsv_image, new Point(210,210), 10, new Scalar(100,10,10),3);  
					data=hsv_image.get(210, 210);  
					Core.putText(hsv_image,String.format("("+String.valueOf(data[0])+","+String.valueOf(data[1])+","+String.valueOf(data[2])+")"),new Point(30, 30) , 3 //FONT_HERSHEY_SCRIPT_SIMPLEX  
							,1.0,new Scalar(100,10,10,255),3);  

					distance.convertTo(distance, CvType.CV_8UC1);  
					Core.line(distance, new Point(150,50), new Point(202,200), new Scalar(100)/*CV_BGR(100,10,10)*/, 3);  
					Core.circle(distance, new Point(210,210), 10, new Scalar(100),3);  
					data=(double[])distance.get(210, 210);  
					//getCoordinates(thresholded);
					Core.putText(distance,String.format("("+String.valueOf(data[0])+")"),new Point(30, 30) , 3 //FONT_HERSHEY_SCRIPT_SIMPLEX  
							,1.0,new Scalar(100),3);   
					//-- 5. Display the image  

					//System.out.println(data[1]);
					panel1.setimagewithMat(webcam_image);  
  
					frame1.repaint();  


				}  
				else  
				{  
					System.out.println(" --(!) No captured frame -- Break!");  
					break;  
				}  
			}  
		}  

	}
} 
