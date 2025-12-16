#!/bin/bash

echo "========================================"
echo "    CITIZEN CARD DATABASE VIEWER"
echo "========================================"
echo

cd desktop

echo "Chọn cách xem database:"
echo "1. Mở H2 Web Console (khuyến nghị)"
echo "2. In dữ liệu ra console"
echo "3. Xem thống kê database"
echo "4. Thoát"
echo

read -p "Nhập lựa chọn (1-4): " choice

case $choice in
    1)
        echo
        echo "Đang khởi động H2 Web Console..."
        java -cp "target/classes:target/dependency/*" citizencard.util.DatabaseViewer console
        ;;
    2)
        echo
        echo "Đang lấy dữ liệu database..."
        java -cp "target/classes:target/dependency/*" citizencard.util.DatabaseViewer print
        echo
        read -p "Nhấn Enter để tiếp tục..."
        ;;
    3)
        echo
        echo "Đang tạo thống kê..."
        java -cp "target/classes:target/dependency/*" citizencard.util.DatabaseViewer stats
        echo
        read -p "Nhấn Enter để tiếp tục..."
        ;;
    4)
        echo "Tạm biệt!"
        exit 0
        ;;
    *)
        echo "Lựa chọn không hợp lệ!"
        ;;
esac