package jp.osdn.gokigen.aira01b.logcat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class LogCatUpdater
{
    LogCatUpdater()
    {
        //
    }

    /**
     *
     * @param ringbuffer : main - メイン ログバッファ, radio - 無線通信や電話に関連するメッセージが含まれるバッファ, events - イベントに関連するメッセージが含まれるバッファ
     * @param logFormat : brief - 優先度 / タグとメッセージ発行元プロセスの PID, process - PID のみ, tag - 優先度 / タグのみ, raw - 生のログ, time - 日付、起動時刻、優先度 / タグ、メッセージ発行元プロセスの PID , threadtime - 日付、起動時刻、優先度、タグ、メッセージ発行元スレッドの PID および TID, long - すべてのメタデータ フィールド
     * @param filterSpec　:  レベル : SFEWIDV
     * @param filterString : 指定した文字列がログに含まれている場合に表示
     * @param filterRegEx :  指定した正規表現の文字列がログに含まれている場合に表示
     * @return ログのリスト
     */

    List<String> getLogCat(String ringbuffer, String logFormat, String filterSpec, String filterString, String filterRegEx)
    {
        final int BUFFER_SIZE = 8192;
        ArrayList<String> listItems = new ArrayList<String>();
        try
        {
            ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add("logcat");
            commandLine.add("-d");       //  -d:  dump the log and then exit (don't block)
            commandLine.add("-b");       //  -b <buffer> : request alternate ring buffer ('main' (default), 'radio', 'events')
            commandLine.add(ringbuffer);  //     <buffer> option.
            commandLine.add("-v");       //  -v <format> :  Sets the log print format, where <format> is one of:
            commandLine.add(logFormat);   //                 brief process tag thread raw time threadtime long
            commandLine.add(filterSpec);  //
            Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()), BUFFER_SIZE);
            String line = null;
            do
            {
                line = bufferedReader.readLine();
                try
                {
                    int filterLength = filterString.length();
                    int filterRegExLength = filterRegEx.length();
                    if (((filterLength == 0)&&(filterRegExLength == 0))||
                            ((filterLength > 0)&&(line.contains(filterString)))||
                            ((filterRegExLength > 0)&&(line.matches(filterRegEx))))
                    {
                        listItems.add(line);
                    }
                }
                catch (Exception ee)
                {
                    ee.printStackTrace();
                }
            } while (line != null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return (listItems);
    }
}
