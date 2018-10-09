package cn.embed.media.manager;
/**
 * 推流过程中监控推流状态
 * @author foxyshang
 *
 */

public interface NetInterface {
	public void disconnected();				//网络连接断开
	public void connected();					//连接网络可以推流
	public void connectError();				//网络连接失败
	public void urlError();					//url错误
	
	
}
