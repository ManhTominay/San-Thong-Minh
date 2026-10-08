function goBackToPreviousPage() {
    const params = new URLSearchParams(window.location.search);
    const returnUrl = params.get("returnUrl");

    if (returnUrl) {
        try {
            const target = new URL(returnUrl, window.location.href);
            if (target.origin === window.location.origin) {
                window.location.replace(target.href);
                return;
            }
        } catch (error) {
            console.warn("Đường dẫn quay lại không hợp lệ:", error);
        }
    }

    if (document.referrer) {
        try {
            const referrer = new URL(document.referrer);
            if (referrer.origin === window.location.origin) {
                window.location.replace(referrer.href);
                return;
            }
        } catch (error) {
            console.warn("Không thể đọc trang trước đó:", error);
        }
    }

    if (window.history.length > 1) {
        window.history.back();
        return;
    }

    window.location.replace("index.html");
}
