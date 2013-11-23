package dev;

public class MyUtil {
	
	public String getArg(String[] args, int argPos, String notFoundMessage){
		if(args.length >= argPos+1){
			return args[argPos];
		}else{
			System.out.println( notFoundMessage );
			System.exit(0);
		}
		return "";
	}
	
	//mostrar mensaje en consola?
	public void localMessage(String m){
		System.out.println(m);
	}
	
	public String[] getRestOfArgs(String[] args, int argPos, String notFoundMessage){
		String[] resp = new String[args.length - argPos];
		
		for(int i = 0; i < resp.length; i++){
			resp[i] =  args[i + argPos];
		}
		
		if(resp.length == 0){
			localMessage(notFoundMessage);
		}
		
		return resp;
	}
}
