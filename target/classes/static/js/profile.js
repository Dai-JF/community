$(function () {
  $(".follow-btn").click(follow);
});

function follow() {
  var btn = this;
  if ($(btn).hasClass("btn-info")) {
    // 关注
    $.post(
        CONTEXT_PATH + "/follow",
        //entityId": $(btn).prev().val():获取前一个节点【隐藏域】中id
        {"entityType": 3, "entityId": $(btn).prev().val()},
        function (data) {
          data = $.parseJSON(data);
          if (data.code === 0) {
            window.location.reload();
          } else {
            alert(data.msg);
          }
        }
    );
  } else {
    // 取消关注
    $.post(
        CONTEXT_PATH + "/unfollow",
        {"entityType": 3, "entityId": $(btn).prev().val()},
        function (data) {
          data = $.parseJSON(data);
          if (data.code === 0) {
            window.location.reload();
          } else {
            alert(data.msg);
          }
        }
    )
    ;
  }
}
