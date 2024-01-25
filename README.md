# AirA01b

An android application to control the Olympus Air A01.

This document is written in Japanese.

-----------

AirA01b は、デジタルカメラ OLYMPUS AIR A01 にWi-Fi/Bluetooth経由で接続し制御する Androidアプリケーションです。横型レイアウトの[AirA01a](https://github.com/MRSa/AirA01a)とは異なり、縦型のアプリケーションで、画面下部の表示をスマートフォン内蔵のカメラの表示やOlympus Air A01の設定情報と表示パネル、お手本画像の表示など、お好みの機能を割り当てることができます。標準はスマートフォン内蔵のカメラ表示で、シャッターボタンを押すと、Olympus Air A01で撮影するのと同時にスマートフォン内蔵カメラでも撮影を行います。

-----------

## 制御対象カメラ

[**OLYMPUS AIR A01**](https://jp.omsystem.com/cms/record/dslr/a01/index.pdf)

- [「OLYMPUS AIR A01」 は 2018年 3月 31日をもって販売を終了いたしました。](https://digital-faq.jp.omsystem.com/faq/public/app/servlet/relatedqa?QID=005796)

## OlympusCameraKitについて

AirA01b は、OlympusCameraKit を使用してOLYMPUS AIR A01と通信を行います。そのため、以下の「SDKダウンロード許諾契約書」の条件に従います。

- [EULA_OlympusCameraKit_ForDevelopers_jp.pdf](https://github.com/MRSa/gokigen/blob/5ec908fdbe16c4de9e37fe90d70edc9352b6f948/osdn-svn/Documentations/miscellaneous/EULA_OlympusCameraKit_ForDevelopers_jp.pdf)

## OpenCVについて

AirA01b は、フォーカスアシスト機能を実現するために、[OpenCV](https://opencv.org/)を使用しています。そのため、[OpenCVのライセンス](https://opencv.org/license/)の条件に従います。
(使用しているOpenCVのバージョンは 3 であるため、[3-clause BSD License](https://github.com/opencv/opencv/blob/4.4.0/LICENSE)が適用されます。)

-----------
