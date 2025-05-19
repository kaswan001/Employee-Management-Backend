package net.javaspring.ems.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import net.javaspring.ems.dto.AuditApproveDto;
import net.javaspring.ems.dto.AuditDto;
import net.javaspring.ems.entity.Audit;
import net.javaspring.ems.entity.AuditApprove;
import net.javaspring.ems.entity.Role;
import net.javaspring.ems.entity.User;
import net.javaspring.ems.exception.ResourceNotFoundException;
import net.javaspring.ems.exception.UserPermissionNotAllowedException;
import net.javaspring.ems.repository.AuditApproveRepository;
import net.javaspring.ems.repository.AuditRepository;
import net.javaspring.ems.repository.UserRepository;
import net.javaspring.ems.security.JwtTokenProvider;
import net.javaspring.ems.service.AuditService;
import net.javaspring.ems.utils.AuditType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AuditServiceImpl implements AuditService {

    private AuditRepository auditRepository;

    private AuditApproveRepository auditApproveRepository;

    private UserRepository userRepository;

    private JwtTokenProvider jwtTokenProvider;

    private ModelMapper modelMapper;


    @Override
    public AuditDto createAudit(AuditDto auditDto, String username) {

        User user = userRepository.findByUsernameOrEmail(username,username).orElseThrow(() -> new ResourceNotFoundException("User","id" + username ,auditDto.getUserId()));
        auditDto.setUserId(user.getId());

        Audit audit = new Audit();
        audit.setAmountMoney(auditDto.getAmountMoney());
        audit.setTitle(auditDto.getTitle());
        audit.setContent(auditDto.getContent());
        audit.setRequireAllApprovalPassing(auditDto.isRequireAllApprovalPassing());
        audit.setAllowedToLeapfrog(auditDto.isAllowedToLeapfrog());
        audit.setRequirePeerReview(auditDto.isRequirePeerReview());
        audit.setUser(user);

        AuditType auditType = AuditType.fromValue(auditDto.getAuditType());
        audit.setAuditType(auditType);
        audit.setStatus(1);

        List<AuditApprove> auditApproveList= createAuditApprovalList(audit, user);
        audit.setApprovals(auditApproveList);
        audit.setCreateTime(LocalDateTime.now());
        Audit savedAudit = auditRepository.save(audit);

        auditDto.setApprovals(convertAuditApproveListToDto(savedAudit.getApprovals()));
        auditDto.setStatus(audit.getStatus());
        auditDto.setCreateTime(audit.getCreateTime());
        return auditDto;
    }

    @Override
    public List<AuditApprove> getAllApprovalsById(Long auditId) {

        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new ResourceNotFoundException("Audit","id",auditId));
        return audit.getApprovals();
    }

    @Override
    public AuditDto getAuditById(Long auditId) {

        Audit audit = auditRepository.findById(auditId).orElseThrow(() -> new ResourceNotFoundException("Audit","id",auditId));
        return convertAuditToAuditDto(audit);
    }

    @Override
    public List<AuditDto> getAllAudit() {

        List<Audit> auditList = auditRepository.findAll();
        return auditList.stream().map(audit -> convertAuditToAuditDto(audit)).collect(Collectors.toList());
    }

    @Override
    public AuditDto userApproveAudit(AuditApproveDto auditApproveDto, String username) {

        String approved = auditApproveDto.getApproved();
        User user = userRepository.findByUsernameOrEmail(username,username).orElseThrow(() -> new ResourceNotFoundException("User","id",auditApproveDto.getUserId()));
        Audit audit = auditRepository.findById(auditApproveDto.getAuditId()).orElseThrow(() -> new ResourceNotFoundException("Audit", "id", auditApproveDto.getAuditId()));
        auditApproveDto.setUserId(user.getId());

        if(audit.getUser() == user){

            if((audit.getAuditType().getLevel() <= convertAllRolesToMaxNum(user.getRoles()) && !audit.isRequirePeerReview()) ||
                    (audit.getAuditType().getLevel() < convertAllRolesToMaxNum(user.getRoles()) && audit.isRequirePeerReview())){
                audit.setStatus(0);
                audit.setUpdateTime(LocalDateTime.now());
                Audit savedAudit = auditRepository.save(audit);
                return modelMapper.map(savedAudit,AuditDto.class);
            }else{
                throw new UserPermissionNotAllowedException((long)1 , user.getId());
            }
        }

        checkUserRoleForApprove(user,audit);

        AuditApprove newAuditApprove = audit.getApprovals().stream()
                .filter(auditApprove -> auditApprove.getAudit().getId() == audit.getId() && auditApprove.getUser() == user)
                .findFirst()
                .orElse(null);
        newAuditApprove.setApproved(auditApproveDto.getApproved());
        newAuditApprove.setApprovedStatus("complete");
        newAuditApprove.setApprovalTime(LocalDateTime.now());
        auditApproveRepository.save(newAuditApprove);


        Audit savedAudit = auditRepository.findById(auditApproveDto.getAuditId()).orElseThrow(() -> new ResourceNotFoundException("Audit", "id", auditApproveDto.getAuditId()));


        if(savedAudit.isAllowedToLeapfrog() && savedAudit.getAuditType().getLevel() < convertAllRolesToMaxNum(user.getRoles())){
            if(newAuditApprove.getApproved().equals("approved")) {
                savedAudit.setStatus(2);
                savedAudit.setApprovals(updateApprovalListStatus(audit.getApprovals(), "cancel"));
            }
        }else{
            savedAudit = checkAuditStatus(audit,newAuditApprove);
        }

        savedAudit.setUpdateTime(LocalDateTime.now());
        return convertAuditToAuditDto(auditRepository.save(savedAudit));
    }

    private Audit checkAuditStatus(Audit audit, AuditApprove newAuditApprove){
        List<AuditApprove> auditApprovals = audit.getApprovals();

        boolean checkCurLevelAllFalse = auditApprovals.stream()
                .filter(appro -> appro.getAuditLevelOrder() == newAuditApprove.getAuditLevelOrder())
                .allMatch(appro -> appro.getApproved().equals("refused"));

        boolean checkCurLevelAllTrue = auditApprovals.stream()
                .filter(appro -> appro.getAuditLevelOrder() == newAuditApprove.getAuditLevelOrder())
                .allMatch(appro -> appro.getApproved().equals("approved"));

        if((audit.isRequireAllApprovalPassing() && newAuditApprove.getApproved().equals("refused")) || (checkCurLevelAllFalse)){
            audit.setStatus(3);
            auditApprovals = updateApprovalListStatus(auditApprovals, "cancel");
        }
        else if(auditApprovals.stream().allMatch(appro -> appro.getApproved().equals("approved"))){
            audit.setStatus(2);
        }else if((!audit.isRequireAllApprovalPassing() && newAuditApprove.isLastLevel() && newAuditApprove.getApproved().equals("approved"))){
            auditApprovals = updateApprovalListStatus(auditApprovals, "cancel");
            audit.setStatus(2);
        }
        else if(!audit.isRequireAllApprovalPassing() && newAuditApprove.getApproved().equals("approved")){
            auditApprovals = auditApprovals.stream()
                    .peek(appro -> {
                        if (appro.getAuditLevelOrder() == newAuditApprove.getAuditLevelOrder() && appro.getApproved().equals("")) {
                            appro.setApprovedStatus("cancel");
                        }
                    })
                    .collect(Collectors.toList());

            auditApprovals = auditApprovals.stream()
                    .peek(appro -> {
                        if (appro.getAuditLevelOrder() == newAuditApprove.getAuditLevelOrder() + 1 && appro.getApprovedStatus().equals("waiting")) {
                            appro.setApprovedStatus("process");
                        }
                    })
                    .collect(Collectors.toList());
        }
        else if(audit.isRequireAllApprovalPassing() && checkCurLevelAllTrue){
            auditApprovals = auditApprovals.stream()
                    .peek(appro -> {
                        if (appro.getAuditLevelOrder() == newAuditApprove.getAuditLevelOrder() + 1 && appro.getApprovedStatus().equals("waiting")) {
                            appro.setApprovedStatus("process");
                        }
                    })
                    .collect(Collectors.toList());
        }

        audit.setApprovals(auditApprovals);
        return auditRepository.save(audit);
    }

    public List<AuditApprove> updateApprovalListStatus(List<AuditApprove> approvals, String status) {
        return approvals.stream()
                .map(appro -> {
                    if (appro.getApprovedStatus().equals("waiting") || appro.getApprovedStatus().equals("process")) {
                        appro.setApprovedStatus(status);
                    }
                    return appro;
                })
                .collect(Collectors.toList());
    }

    private boolean checkUserRoleForApprove(User user, Audit audit){

        if(audit.getStatus() != 1){
            throw new UserPermissionNotAllowedException((long)0, user.getId());
        }

        int userLevel = convertAllRolesToMaxNum(user.getRoles());
        int applicantLevel =  convertAllRolesToMaxNum(audit.getUser().getRoles());

        int lowestLevel = audit.isRequirePeerReview() ? applicantLevel : applicantLevel + 1;

        boolean userLevelRequirement = audit.isRequirePeerReview() ? userLevel >= applicantLevel : userLevel > applicantLevel;
        boolean repeatedApprove = audit.getApprovals().stream().anyMatch(approval -> (approval.getUser().equals(user) && !approval.getApproved().equals("")));

        if(!userLevelRequirement || repeatedApprove){
            throw new UserPermissionNotAllowedException((long)2, user.getId());
        }

        if(!audit.isAllowedToLeapfrog() && userLevel != lowestLevel){
            List<AuditApprove> lastLevelApprovals = auditApproveRepository.findAllCurrentLevelApprovals(audit,userLevel - 1);

            if(audit.isRequireAllApprovalPassing() && lastLevelApprovals.stream().anyMatch(approval -> approval.getApprovedStatus().equals("waiting"))){
                throw new UserPermissionNotAllowedException((long)3, user.getId());
            }
            else if(!audit.isRequireAllApprovalPassing() && lastLevelApprovals.stream().noneMatch(approval -> approval.getApprovedStatus().equals("complete"))){
                throw new UserPermissionNotAllowedException((long)4, user.getId());
            }
        }

        return true;
    }

    private List<AuditApprove> createAuditApprovalList(Audit audit, User user){
        List<AuditApprove> auditApproveList = new ArrayList<>();

        boolean isLastLevel = false;

        int peerReview = audit.isRequirePeerReview() ? 0 : 1;

        int startLevel = convertAllRolesToMaxNum(user.getRoles()) + peerReview;

        for (int level = startLevel ; level <= audit.getAuditType().getLevel(); level++){

            if(level == audit.getAuditType().getLevel()) isLastLevel = true;
            String status = "waiting";

            if(audit.isAllowedToLeapfrog() || level == startLevel) status = "process";

            List<User> curLevelUser = userRepository.findByRoleName(convertNumToRoleName(level));
            for(User curUser : curLevelUser){
                AuditApprove auditApprove = createAuditApprove(audit,curUser,level,isLastLevel,status);
                auditApproveList.add(auditApprove);
            }
        }
        return auditApproveList;
    }

    private AuditApprove createAuditApprove(Audit audit, User user, int level ,boolean isLastLevel, String status){
        AuditApprove auditApprove = new AuditApprove();
        auditApprove.setAudit(audit);
        auditApprove.setUser(user);
        auditApprove.setApprovedStatus(status);
        auditApprove.setAuditLevelOrder(level);
        auditApprove.setLastLevel(isLastLevel);
        auditApprove.setApproved("");

        return auditApprove;

    }

    private List<AuditApproveDto> convertAuditApproveListToDto(List<AuditApprove> list){
        List<AuditApproveDto> dtoList = new ArrayList<>();

        for (AuditApprove auditApprove : list){
            AuditApproveDto auditApproveDto = new AuditApproveDto();
            auditApproveDto.setAuditId(auditApprove.getAudit().getId());
            auditApproveDto.setUserId(auditApprove.getUser().getId());
            auditApproveDto.setApprovedStatus(auditApprove.getApprovedStatus());
            auditApproveDto.setApproved("");
            auditApproveDto.setAuditLevelOrder(auditApprove.getAuditLevelOrder());
            auditApproveDto.setLastLevel(auditApprove.isLastLevel());

            dtoList.add(auditApproveDto);
        }

        return dtoList;
    }


    private int convertAllRolesToMaxNum(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResourceNotFoundException("User", "roleNotFound", null);
        }

        int max = -1;

        for (Role role : roles){
            String name = role.getName().toLowerCase();

            switch (name) {
                case "regular":
                    max = Math.max(1,max);
                    break;
                case "group_leader":
                    max = Math.max(2,max);
                    break;
                case "manager":
                    max = Math.max(3,max);
                    break;
                case "director":
                    max = Math.max(4,max);
                    break;
                case "president":
                    max = Math.max(5,max);
                    break;
                case "role_admin": break;// 排除admin
                case "role_user": break;// 排除user

            }
        }
        return max;
    }

    private String convertNumToRoleName(int num){
        return switch (num) {
            case 1 -> "REGULAR";
            case 2 -> "GROUP_LEADER";
            case 3 -> "MANAGER";
            case 4 -> "DIRECTOR";
            case 5 -> "PRESIDENT";
            default -> throw new ResourceNotFoundException("Role", "RoleNum", (long) num);
        };
    }

    private AuditDto convertAuditToAuditDto(Audit audit){
        AuditDto auditDto = new AuditDto();

        auditDto.setAuditType(audit.getAuditType().getLevel());
        auditDto.setApprovals(convertAuditApproveListToDto(audit.getApprovals()));
        auditDto.setAmountMoney(audit.getAmountMoney());
        auditDto.setTitle(audit.getTitle());
        auditDto.setContent(audit.getContent());
        auditDto.setAllowedToLeapfrog(audit.isAllowedToLeapfrog());
        auditDto.setRequirePeerReview(audit.isRequirePeerReview());
        auditDto.setRequireAllApprovalPassing(audit.isRequireAllApprovalPassing());
        auditDto.setStatus(audit.getStatus());
        auditDto.setUserId(audit.getId());
        auditDto.setCreateTime(audit.getCreateTime());
        auditDto.setUpdateTime(audit.getUpdateTime());

        return auditDto;
    }



}
