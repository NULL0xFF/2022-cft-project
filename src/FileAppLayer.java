import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileAppLayer extends BaseLayer {

    private final ArrayList<Boolean> ackCheckList = new ArrayList<>();
    private FileAppHeader header;
    private byte[] fragBytes;
    private int fragCount = 0;

    public FileAppLayer(String name) {
        super(name);
        resetHeader();
        ackCheckList.add(true);
    }

    public void resetHeader() {
        header = new FileAppHeader();
    }

    public byte[] removeHeader(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength - 12];
        System.arraycopy(dataArray, 12, byteBuffer, 0, arrayLength - 12);
        return byteBuffer;
    }

    private byte[] objectToByte(FileAppHeader header, byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength + 12];

        for (int index = 0; index < 4; index++)
            byteBuffer[index] = header.totalLength[index];
        for (int index = 4; index < 6; index++)
            byteBuffer[index] = header.totalLength[index];
        byteBuffer[6] = header.messageType;
        byteBuffer[7] = header.unused;
        for (int index = 8; index < 12; index++)
            byteBuffer[index] = header.sequenceNumber[index];

        if (arrayLength >= 0)
            System.arraycopy(dataArray, 0, byteBuffer, 12, arrayLength);

        return byteBuffer;
    }

    private byte[] integerToByte4(int value) {
        byte[] byteBuffer = new byte[4];

        byteBuffer[0] |= (byte) ((value & 0xFF000000) >> 24);
        byteBuffer[1] |= (byte) ((value & 0xFF0000) >> 16);
        byteBuffer[2] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[3] |= (byte) (value & 0xFF);

        return byteBuffer;
    }

    private int byte4ToInteger(byte value1, byte value2, byte value3, byte value4) {
        return (value1 << 24) | (value2 << 16) | (value3 << 8) | (value4);
    }

    private void waitACK() { // ACK 체크
        while (ackCheckList.size() <= 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ackCheckList.remove(0);
    }

    // 단편화를 진행하는 함수.
    private void fragSend(byte[] dataArray, int arrayLength) {

    	byte[] byteBuffer = new byte[1448];
        int i = 0;
        header.totalLength = integerToByte4(arrayLength);
        header.fragType = integerToByte4(0x01);

        // First Send (fragType == 0x01)
        // 1448byte로 data를 나누고, 하위 계층에 전송한다.
        System.arraycopy(dataArray, 0, byteBuffer, 0, 1448);
        byteBuffer = objectToByte(header, byteBuffer, 1448);
        this.getUnderLayer().send(byteBuffer, byteBuffer.length);
    	// +전송 후 Progress bar 증가

        // Second Send (fragType == 0x02)
        // 최대크기가 1448로 고정되고, maxLength에 도달하기 전 까지만 단편화 전송.
        header.totalLength = integerToByte4(1448);
        header.fragType = integerToByte4(0x02);
        int maxLength = arrayLength / 1448;

        for (i = 1; i < maxLength; i++) {
        	this.waitACK();	// ACK 대기.
        	if((i+1 == maxLength) && (arrayLength % 1448 == 0)) {
        		header.fragType = integerToByte4(0x03);
        	}
        	System.arraycopy(dataArray, 1448*i, byteBuffer, 0, 1448);
        	byteBuffer = objectToByte(header, byteBuffer, 1448);
        	this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        	// +전송 후 Progress bar 증가
        }

        // Final Send (fragType == 0x03)
        // 단편화 마지막 자투리 data 전송.
        if ((arrayLength % 1448) != 0) {
        	this.waitACK();
        	byteBuffer = new byte[arrayLength % 1448];
            header.totalLength = integerToByte4(arrayLength % 1448);

            System.arraycopy(
            		dataArray,
            		arrayLength-(arrayLength%1448),
            		byteBuffer,
            		0,
            		arrayLength%1448
            		);
            byteBuffer = objectToByte(header, byteBuffer, byteBuffer.length);
            this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        	// +전송 후 Progress bar 증가
        }
    }

    @Override
    public synchronized boolean send(String filename) {
        File file = new File(filename);
        FileInputStream fileInputStream = null;
        byte[] fileBuffer = new byte[Math.toIntExact(file.length())];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBuffer);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetHeader();
        header.totalLength = integerToByte4(fileBuffer.length);
        header.messageType = (byte) (0x00);

        this.waitACK();


        // Send Message First (messageType == 0x00)
        // 기본 헤더 정보(파일명)을 먼저 보내는 것.
        byte[] byteBuffer = filename.getBytes();
        byteBuffer = this.objectToByte(header, byteBuffer, byteBuffer.length);
        getUnderLayer().send(byteBuffer, byteBuffer.length);


        header.messageType = (byte) (0x01);

        // 선택된 파일의 길이가 1448bytes이하면, 단편화 없이 전송
        if (file.length() < 1448) {
        	byteBuffer = this.objectToByte(header, byteBuffer, byteBuffer.length);
        	this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        }
        else {
        	this.fragSend(byteBuffer, byteBuffer.length);
        }


        this.waitACK();

        // TODO (Un)Fragmented Send
        return false;
    }

    public synchronized boolean receive(byte[] input) {

    	/*
    	 * ① 전달받은 첫 조각을 통해 단편화 여부를 확인
    	 * ② 단편화가 되어있지 않다면, messageType 확인해서 패킷을 두 번 받은 후 파일 저장
    	 * ③ 단편화가 되어있다면, buffer를 활용하여 파일이 모두 전달될 때까지 덧붙임
    	 * ④ 모든 조각을 전달받았다면, buffer에 쓰여진 내용을 파일로 저장
    	 */
    	
    	// 기본적으로 ACK를 받는 경우를 검사합니다. 
    	if (input == null) {
    		ackCheckList.add(true);
    		return true;
    	}
    	
    	int totalLength = input[0];
    	byte fragType = input[1];
    	byte messageType = input[2];
    	
    	byte[] dataBuffer = this.removeHeader(input, input.length);
    	
    	File file = new File("File name is...");
        FileInputStream fileInputStream = null;
        byte[] fileBuffer = new byte[Math.toIntExact(file.length())];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBuffer);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
        // File name Receive (messageType == 0x00)
        // 모든 Receive동작은 File name을 먼저 받는다.
		if (messageType == 0x00) {
			dataBuffer = this.removeHeader(input, input.length);
			String fileName = new String(dataBuffer, StandardCharsets.UTF_8);
			
			// File Name 새롭게 기록.
			File newFile = new File(fileName);
			file.renameTo(newFile);
		}
    	
    	// ① 전달받은 첫 조각을 통해 단편화 여부를 확인
    	// ③ 단편화가 되어있다면, buffer를 활용하여 파일이 모두 전달될 때까지 덧붙임
    	if (messageType == 0x01) {
    		// First Fragment Receive (fragType == 0x01)
    		if (fragType == 0x01) {
                int length = dataBuffer.length;
                this.fragBytes = new byte[length];
                this.fragCount = 1;
                dataBuffer = this.removeHeader(input, input.length);
                System.arraycopy(dataBuffer, 0, this.fragBytes, 0, 1448);
    		}
    		// Second Fragment Receive (fragType == 0x01)
    		else if (fragType == 0x02) {
    			dataBuffer = this.removeHeader(input, input.length);
                System.arraycopy(dataBuffer, 0, this.fragBytes, this.fragCount * 1448, 1448);
                this.fragCount++;
    		}
    		// Final Fragment Receive (fragType == 0x02)
    		else if (fragType == 0x03) {
    			dataBuffer = this.removeHeader(input, input.length);
                System.arraycopy(
                		dataBuffer, 
                		0, 
                		this.fragBytes, 
                		this.fragCount * 1448,
                        this.byte4ToInteger(input[0], input[1], input[3], input[4]));
                this.fragCount++;

                // ④ 모든 조각을 전달받았다면, buffer에 쓰여진 내용을 파일로 저장.
                try {
					FileOutputStream output = new FileOutputStream(file);
					try {
						output.write(fileBuffer);
						output.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
    		}
    	}

    	// ② 단편화가 되어있지 않다면, messageType 확인해서 패킷을 두 번 받은 후 파일 저장
    	else if(fragType == 0x00) {
    		
    		// 데이터 패킷을 받는 경우. (messageType == 0x01)
    		if (messageType == 0x01) {
    			
    			// 파일에 데이터를 기록한다.
    			dataBuffer = this.removeHeader(input, input.length);
    			System.arraycopy(
                		dataBuffer, 
                		0, 
                		this.fragBytes, 
                		this.fragCount * 1448,
                        this.byte4ToInteger(input[0], input[1], input[3], input[4]));
    			
    			// buffer에 쓰여진 내용을 file로 저장.
    			try {
					FileOutputStream output = new FileOutputStream(file);
					try {
						output.write(fileBuffer);
						output.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			
    		}
    		
    	}
    	
    	
		return false;
    }

    private class FileAppHeader {
        byte[] totalLength;
        /**
         * Fragmentation
         */
        byte[] fragType;
        /**
         * Type
         * <p>
         * String (0x00) File (0x01)
         */
        byte messageType;
        byte unused;
        /**
         * Data Sequence Number
         */
        byte[] sequenceNumber;
        byte[] data;

        public FileAppHeader() {
            this.totalLength = new byte[4];
            this.fragType = new byte[2];
            this.messageType = 0x00;
            this.unused = 0x00;
            this.sequenceNumber = new byte[4];
            this.data = null;
        }
    }
}